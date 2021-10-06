package com.ing.zkflow.common.client.flows

import com.ing.zkflow.testing.fixtures.contract.TestContract
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.corda.core.contracts.StateRef
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.node.StartedMockNode

fun checkVault(
    tx: SignedTransaction,
    sender: StartedMockNode?,
    receiver: StartedMockNode
) {

    // Sender should have CONSUMED input state marked in its vault
    sender?.let { it ->

        val state = it.services.vaultService
            .queryBy(
                contractStateType = TestContract.TestState::class.java,
                criteria = QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.CONSUMED)
            ).states.find { state -> state.ref == tx.inputs.single() }

        state shouldNotBe null
    }

    // Receiver should have UNCONSUMED output state in its vault
    val actualState = receiver.services.vaultService
        .queryBy(
            contractStateType = TestContract.TestState::class.java,
            criteria = QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED)
        ).states.single()

    actualState shouldBe tx.tx.outRef(0)
}

fun checkVault(
    party: StartedMockNode,
    status: Vault.StateStatus,
    stateRef: StateRef
) {
    val state = party.services.vaultService
        .queryBy(
            contractStateType = TestContract.TestState::class.java,
            criteria = QueryCriteria.VaultQueryCriteria()
                .withStatus(status)
                .withStateRefs(listOf(stateRef))
        ).states.find { state -> state.ref == stateRef }

    state shouldNotBe null
}
