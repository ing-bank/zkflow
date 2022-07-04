package com.ing.zkflow.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import com.ing.zkflow.common.versioning.ContractStateVersionFamilyRegistry
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import com.ing.zkflow.util.requireNotNull
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

@InitiatingFlow
class ZKUpgradeStateToNextFlow(
    private val stateToUpgrade: StateAndRef<ContractState>
) : FlowLogic<StateAndRef<ContractState>>() {

    @Suspendable
    override fun call(): StateAndRef<ContractState> {
        val zkTxStorage: ZKVerifierTransactionStorage =
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)

        val isPrivate = zkTxStorage.getTransaction(stateToUpgrade.ref.txhash)
            .requireNotNull { "Can't upgrade $stateToUpgrade: can't find transaction with id ${stateToUpgrade.ref.txhash}" }
            .tx
            .isPrivateComponent(ComponentGroupEnum.OUTPUTS_GROUP, stateToUpgrade.ref.index)

        val me = serviceHub.myInfo.legalIdentities.single().anonymise()

        val currentStateClass = stateToUpgrade.state.data::class

        val versionFamily = ContractStateVersionFamilyRegistry.familyOf(currentStateClass)
            ?: error("This node does not recognize $stateToUpgrade as belonging to a ${VersionedContractStateGroup::class.simpleName} it knows.")

        val nextVersionKClass = versionFamily.next(currentStateClass)
            ?: error("Could not upgrade $stateToUpgrade to next version. It is already the latest version supported by this node.")

        logger.info("Upgrading $currentStateClass to $nextVersionKClass")

        val nextVersionInstance =
            nextVersionKClass.constructors.firstOrNull { it.parameters.size == 1 && it.parameters.first().type.classifier == currentStateClass }
                .requireNotNull {
                    "Can't upgrade $currentStateClass to $nextVersionKClass: $nextVersionKClass has no constructor for $currentStateClass."
                }
                .call(stateToUpgrade.state.data) as? ContractState
                ?: error("Can't upgrade $currentStateClass to $nextVersionKClass: $nextVersionKClass is not a ContractState.")

        val upgradeCommand = getUpgradeCommand(currentStateClass, nextVersionKClass, isPrivate)

        // Build the upgrade tx
        val builder = ZKTransactionBuilder(stateToUpgrade.state.notary)
            .addInputState(stateToUpgrade)
            .addOutputState(nextVersionInstance, stateToUpgrade.state.contract, stateToUpgrade.state.constraint)
            .addCommand(upgradeCommand, me.owningKey)

        // Sign it.
        val stx = serviceHub.signInitialTransaction(builder)

        // Collect signatures of all participants of the state to upgrade. This discloses state contents to these participants.
        // It is assumed that they were already aware of the state contents as a participant.
        // Alternative could be to introduce a `privateParticipants` field on a `ZKContractState` interface.
        val participantSessions = stateToUpgrade.state.data.participants
            .filterNot { participant -> serviceHub.myInfo.legalIdentities.any { it.owningKey == participant.owningKey } }
            .map { initiateFlow(it) }
        val fullySignedTx = subFlow(ZKCollectSignaturesFlow(stx, participantSessions))

        subFlow(ZKFinalityFlow(fullySignedTx, publicSessions = emptyList(), privateSessions = participantSessions))

        return stx.tx.outRef(0)
    }
}
