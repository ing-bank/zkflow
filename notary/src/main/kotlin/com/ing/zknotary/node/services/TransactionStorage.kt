package com.ing.zknotary.node.services

import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.dependencies
import net.corda.core.node.services.TransactionStorage
import net.corda.core.transactions.SignedTransaction
import net.corda.node.services.DbTransactionsResolver

/**
 * Collects all verified transaction chains from local storage for each StateRef provided.
 * This should always be called after ResolveTransactionsFlow, to prevent an exception because of a missing transaction
 *
 * The returned list of SecureHashes is topologically ordered, so that any dependencies of a transaction
 * always appear first in the list. This makes it possible to verify all transactions in the list from
 * left to right and be sure that all dependencies for each transaction are always already known and verified.
 */
fun TransactionStorage.collectVerifiedDependencies(
    stateRefs: List<StateRef>,
    block: ((stx: SignedTransaction) -> Unit)? = null
): List<SecureHash> {
    val txHashes = stateRefs.map { it.txhash }

    // Keep things unique but ordered, for unit test stability.
    val nextRequests = LinkedHashSet<SecureHash>(txHashes)
    val topologicalSort = DbTransactionsResolver.TopologicalSort()

    while (nextRequests.isNotEmpty()) {
        // Don't re-fetch the same tx when it's referenced multiple times in the graph we're traversing.
        nextRequests.removeAll(topologicalSort.transactionIds)
        if (nextRequests.isEmpty()) {
            break
        }

        val txFromDB = checkNotNull(getTransaction(nextRequests.first())) {
            "Transaction with id ${nextRequests.first()} is missing from local storage. " +
                "Please download it from peers with ResolveTransactionsFlow before using this function"
        }

        if (block != null) block(txFromDB)

        val dependencies = txFromDB.dependencies
        topologicalSort.add(txFromDB.id, dependencies)
        nextRequests.addAll(dependencies)
    }
    return topologicalSort.complete()
}
