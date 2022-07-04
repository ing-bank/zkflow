package com.ing.zkflow.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.versioning.ContractStateVersionFamilyRegistry
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.versioning.isVersion
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

@InitiatingFlow
class ZKUpgradeStateFlow(
    private val stateToUpgrade: StateAndRef<ContractState>,
    private val versionToUpgradeTo: Int
) : FlowLogic<StateAndRef<ContractState>>() {

    @Suspendable
    override fun call(): StateAndRef<ContractState> {
        val versionFamily = ContractStateVersionFamilyRegistry.familyOf(stateToUpgrade.state.data::class)
            ?: error("This node does not recognize $stateToUpgrade as belonging to a ${VersionedContractStateGroup::class.simpleName} it knows.")

        require(versionFamily.supportsVersion(versionToUpgradeTo)) {
            "Could not upgrade $stateToUpgrade to version $versionToUpgradeTo. The latest version supported by this node is version ${versionFamily.highestVersionSupported}"
        }

        var currentInput = stateToUpgrade
        while (!currentInput.state.data.isVersion(versionToUpgradeTo)) {
            currentInput = subFlow(ZKUpgradeStateToNextFlow(currentInput))
        }

        return currentInput
    }
}
