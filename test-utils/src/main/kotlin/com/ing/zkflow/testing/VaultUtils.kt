package com.ing.zkflow.testing

import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.ZKTransactionResolutionException
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.corda.core.contracts.StateAndRef
import net.corda.core.internal.indexOfOrThrow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.node.StartedMockNode
import kotlin.test.assertFailsWith

public fun StartedMockNode.checkNotPresentInVault(
    stateAndRef: StateAndRef<*>,
) {
    val state = services.vaultService
        .queryBy(
            contractStateType = stateAndRef.state.data::class.java,
            criteria = QueryCriteria.VaultQueryCriteria()
                .withStateRefs(listOf(stateAndRef.ref))
        ).states.find { state -> state.ref == stateAndRef.ref }

    state shouldBe null
}

public fun StartedMockNode.checkIsPresentInVault(
    stateAndRef: StateAndRef<*>,
    status: Vault.StateStatus
) {
    val state = services.vaultService
        .queryBy(
            contractStateType = stateAndRef.state.data::class.java,
            criteria = QueryCriteria.VaultQueryCriteria()
                .withStatus(status)
                .withStateRefs(listOf(stateAndRef.ref))
        ).states.find { state -> state.ref == stateAndRef.ref }

    state shouldNotBe null
}

public fun StartedMockNode.checkNotPresentInZKStorage(
    stateAndRef: StateAndRef<*>,
) {
    val storage = services.getCordaServiceFromConfig<ZKVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE)
    val tx = storage.getTransaction(stateAndRef.ref.txhash) ?: throw ZKTransactionResolutionException(stateAndRef.ref.txhash)
    assertFailsWith<IllegalArgumentException>("No such element") {
        tx.tx.outputStates.indexOfOrThrow(stateAndRef.state.data)
    }
}

public fun StartedMockNode.checkIsPresentInZKStorage(
    stateAndRef: StateAndRef<*>,
) {
    val storage = services.getCordaServiceFromConfig<ZKVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE)
    val tx = storage.getTransaction(stateAndRef.ref.txhash) ?: throw ZKTransactionResolutionException(stateAndRef.ref.txhash)
    // This does not work: `val state = tx.tx.getOutput(stateAndRef.ref.index)` since for ZKVerifierTransaction
    // the outputs are reindexed: the private outputs are not in the list. So we do a best guess match on type and class equality
    val state = tx.tx.getOutput(tx.tx.outputStates.indexOfOrThrow(stateAndRef.state.data))

    state shouldBe stateAndRef.state.data
}
