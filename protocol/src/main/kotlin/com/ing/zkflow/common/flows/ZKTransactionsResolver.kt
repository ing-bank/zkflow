/*
 * Source attribution:
 *
 * The classes in this file are strongly based on their original non-ZKP counterpart (i.e. without the 'ZK' prefix in the class name) from Corda
 * itself, as defined in the package net.corda.core.flows (https://github.com/corda/corda).
 *
 * Ideally ZKFlow could have extended the Corda classes to add the ZKP resolution only, and leave the rest of the behaviour intact.
 * Unfortunately, Corda's core classes were not implemented with extension in mind, and it was not possible to create this resolver without copying most
 * of the original.
 */
package com.ing.zkflow.common.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.client.flows.FetchZKTransactionsFlow
import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.dependencies
import com.ing.zkflow.common.transactions.verification.ZKTransactionVerifierService
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.internal.TransactionsResolver
import net.corda.core.node.StatesToRecord
import net.corda.core.utilities.debug
import net.corda.core.utilities.seconds
import net.corda.core.utilities.trace
import java.util.Collections

internal class ZKTransactionsResolver(private val flow: ResolveZKTransactionsFlow) : TransactionsResolver {
    private var sortedDependencies: List<SecureHash>? = null
    private val logger = flow.logger

    @Suspendable
    override fun downloadDependencies(batchMode: Boolean) {
        logger.debug { "Downloading dependencies for transactions ${flow.txHashes}" }
        val transactionStorage = flow.serviceHub.getCordaServiceFromConfig<ZKWritableVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE)

        val nextRequests = LinkedHashSet<SecureHash>(flow.txHashes) // Keep things unique but ordered, for unit test stability.
        val topologicalSort = TopologicalSort()
        logger.debug { "DbTransactionsResolver.downloadDependencies(batchMode=$batchMode)" }

        while (nextRequests.isNotEmpty()) {
            logger.debug { "Main fetch loop: size_remaining=${nextRequests.size}" }
            // Don't re-download the same tx when we haven't verified it yet but it's referenced multiple times in the
            // graph we're traversing.
            nextRequests.removeAll(topologicalSort.transactionIds)
            if (nextRequests.isEmpty()) {
                // Done early.
                break
            }

            // Request the standalone transaction data (which may refer to things we don't yet have).
            val (existingTxIds, downloadedTxs) = fetchRequiredTransactions(Collections.singleton(nextRequests.first())) // Fetch first item only
            for (tx in downloadedTxs) {
                val dependencies = tx.dependencies
                topologicalSort.add(tx.id, dependencies)
            }

            var suspended = true
            for (downloaded in downloadedTxs) {
                suspended = false
                val dependencies = downloaded.dependencies
                // Do not keep in memory as this bloats the checkpoint. Write each item to the database.
                transactionStorage.addUnverifiedTransaction(downloaded)

                // The write locks are only released over a suspend, so need to keep track of whether the flow has been suspended to ensure
                // that locks are not held beyond each while loop iteration (as doing this would result in a deadlock due to claiming locks
                // in the wrong order)
                val suspendedViaAttachments = flow.fetchMissingAttachments(downloaded.tx)
                suspended = suspended || suspendedViaAttachments

                // Add all input states and reference input states to the work queue.
                nextRequests.addAll(dependencies)
            }

            // If the flow did not suspend on the last iteration of the downloaded loop above, perform a suspend here to ensure that
            // all data is flushed to the database.
            if (!suspended) {
                FlowLogic.sleep(0.seconds)
            }

            // It's possible that the node has a transaction in storage already. Dependencies should also be present for this transaction,
            // so just remove these IDs from the set of next requests.
            nextRequests.removeAll(existingTxIds)
        }

        sortedDependencies = topologicalSort.complete()
        logger.debug { "Downloaded ${sortedDependencies?.size} dependencies from remote peer for transactions ${flow.txHashes}" }
    }

    override fun recordDependencies(usedStatesToRecord: StatesToRecord) {
        val sortedDependencies = checkNotNull(this.sortedDependencies)
        logger.trace { "Recording ${sortedDependencies.size} dependencies for ${flow.txHashes.size} transactions" }
        val transactionStorage = flow.serviceHub.getCordaServiceFromConfig<ZKWritableVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE)
        for (txId in sortedDependencies) {
            // Retrieve and delete the transaction from the unverified store.
            val (tx, isVerified) = checkNotNull(transactionStorage.getTransactionInternal(txId)) {
                "Somehow the unverified transaction ($txId) that we stored previously is no longer there."
            }
            if (!isVerified) {
                val zkTransactionVerifierService = ZKTransactionVerifierService(
                    flow.serviceHub,
                    flow.serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)
                )

                zkTransactionVerifierService.verify(tx, true)

                transactionStorage.addTransaction(tx)
            } else {
                logger.debug { "No need to record $txId as it's already been verified" }
            }
        }
    }

    // The transactions already present in the database do not need to be checkpointed on every iteration of downloading
    // dependencies for other transactions, so strip these down to just the IDs here.
    @Suspendable
    private fun fetchRequiredTransactions(requests: Set<SecureHash>): Pair<List<SecureHash>, List<SignedZKVerifierTransaction>> {
        val requestedTxs = flow.subFlow(FetchZKTransactionsFlow(requests, flow.otherSide))
        return Pair(requestedTxs.fromDisk.map { it.id }, requestedTxs.downloaded)
    }

    /**
     * Provides a way to topologically sort SignedTransactions represented just their [SecureHash] IDs. This means that given any two transactions
     * T1 and T2 in the list returned by [complete] if T1 is a dependency of T2 then T1 will occur earlier than T2.
     */
    class TopologicalSort {
        private val forwardGraph = HashMap<SecureHash, MutableSet<SecureHash>>()
        val transactionIds = LinkedHashSet<SecureHash>()
        private val nonDupeHash = HashMap<SecureHash, SecureHash>()
        private fun dedupe(sh: SecureHash): SecureHash = nonDupeHash.getOrPut(sh) { sh }

        /**
         * Add a transaction to the to-be-sorted set of transactions.
         * @param txId The ID of the transaction.
         * @param dependentIds the IDs of all the transactions [txId] depends on.
         */
        fun add(txIdp: SecureHash, dependentIds: Set<SecureHash>) {
            val txId = dedupe(txIdp)
            require(transactionIds.add(txId)) { "Transaction ID $txId already seen" }
            dependentIds.forEach {
                // Note that we use a LinkedHashSet here to make the traversal deterministic (as long as the input list is).
                val deDupeIt = dedupe(it)
                forwardGraph.computeIfAbsent(deDupeIt) { LinkedHashSet() }.add(txId)
            }
        }

        /**
         * Return the sorted list of transaction IDs.
         */
        fun complete(): List<SecureHash> {
            val visited = HashSet<SecureHash>(transactionIds.size)
            val result = ArrayList<SecureHash>(transactionIds.size)

            fun visit(txId: SecureHash) {
                if (visited.add(txId)) {
                    forwardGraph[txId]?.forEach(::visit)
                    result += txId
                }
            }

            transactionIds.forEach(::visit)

            return result.apply(Collections::reverse)
        }
    }
}
