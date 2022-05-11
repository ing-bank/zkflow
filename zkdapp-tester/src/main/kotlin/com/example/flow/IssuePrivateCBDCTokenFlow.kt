package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.CBDCContract
import com.example.token.cbdc.CBDCToken
import com.example.token.cbdc.IssuedTokenType
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
class IssuePrivateCBDCTokenFlow(
    private val value: Amount<IssuedTokenType>,
    private val holder: AnonymousParty,
    ) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val state = CBDCToken(value, holder)
        val issueCommand = Command(CBDCContract.IssuePrivate(), holder.owningKey) //
        val stateAndContract = StateAndContract(state, CBDCContract.ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
            .withItems(stateAndContract, issueCommand)
        // builder.enforcePrivateInputsAndReferences(serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE))

        val stx = serviceHub.signInitialTransaction(builder)
        // val fullySignedStx = subFlow(ZKCollectSignaturesFlow(stx, emptyList()))

        subFlow(ZKFinalityFlow(stx, listOf()))

        return stx
    }
}
