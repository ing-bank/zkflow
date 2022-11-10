package com.ing.zkflow.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.versioning.ContractStateVersionFamilyRegistry
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import kotlin.reflect.KClass

@InitiatingFlow
class ZKUpgradeStateFlow(
    private val stateToUpgrade: StateAndRef<ContractState>,
    private val upgradeTo: KClass<out ContractState>,
) : FlowLogic<StateAndRef<ContractState>>() {
    @Suspendable
    override fun call(): StateAndRef<ContractState> {
        val versionFamily = ContractStateVersionFamilyRegistry.familyOf(stateToUpgrade.state.data::class)
            ?: error(
                "ZKFlow does not recognize $stateToUpgrade as belonging to a version group. " +
                    "Please ensure it implements an interface that extends ${VersionedContractStateGroup::class}"
            )

        require(versionFamily.hasMember(upgradeTo)) {
            "Could not upgrade $stateToUpgrade to version $upgradeTo. That version does not exist in version group ${versionFamily.familyClass}"
        }

        val currentVersion = versionFamily.versionOf(stateToUpgrade.state.data::class)
        val versionToUpgradeTo = versionFamily.versionOf(upgradeTo)
        require(versionToUpgradeTo > currentVersion) {
            "Could not upgrade $stateToUpgrade to version $upgradeTo. It is not a higher version"
        }

        var currentInput = stateToUpgrade
        while (currentInput.state.data::class != upgradeTo) {
            currentInput = subFlow(ZKUpgradeStateToNextFlow(currentInput))
        }

        return currentInput
    }
}
