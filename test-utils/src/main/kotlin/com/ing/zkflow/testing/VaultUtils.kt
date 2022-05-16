package com.ing.zkflow.testing

import io.kotest.matchers.shouldNotBe
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.node.StartedMockNode

public fun checkVault(
    node: StartedMockNode,
    stateAndRef: StateAndRef<*>,
    status: Vault.StateStatus
) {
    val state = node.services.vaultService
        .queryBy(
            contractStateType = stateAndRef.state.data::class.java,
            criteria = QueryCriteria.VaultQueryCriteria()
                .withStatus(status)
                .withStateRefs(listOf(stateAndRef.ref))
        ).states.find { state -> state.ref == stateAndRef.ref }

    state shouldNotBe null
}
