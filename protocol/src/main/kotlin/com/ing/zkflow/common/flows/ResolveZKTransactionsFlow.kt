package com.ing.zkflow.common.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.node.services.ZKTransactionsResolver
import net.corda.core.DeleteForDJVM
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.internal.PlatformVersionSwitches
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.debug
import net.corda.core.utilities.trace

/**
 * Resolves transactions for the specified [txHashes] along with their full history (dependency graph) from [otherSide].
 * Each retrieved transaction is validated and inserted into the local transaction storage.
 */
@DeleteForDJVM
class ResolveZKTransactionsFlow constructor(
    val initialTx: SignedTransaction? = null,
    val txHashes: Set<SecureHash>,
    val otherSide: FlowSession
) : FlowLogic<Unit>() {

    @Suppress("MagicNumber")
    @Suspendable
    override fun call() {
        // TODO This error should actually cause the flow to be sent to the flow hospital to be retried
        val counterpartyPlatformVersion =
            checkNotNull(serviceHub.networkMapCache.getNodeByLegalIdentity(otherSide.counterparty)?.platformVersion) {
                "Couldn't retrieve party's ${otherSide.counterparty} platform version from NetworkMapCache"
            }

        val batchMode = counterpartyPlatformVersion >= PlatformVersionSwitches.BATCH_DOWNLOAD_COUNTERPARTY_BACKCHAIN
        logger.debug { "ResolveZKTransactionsFlow.call(): Otherside Platform Version = '$counterpartyPlatformVersion': Batch mode = $batchMode" }

        // Removed fetchMissingAttachments(initialTx) and fetchMissingNetworkParameters(initialTx), seemed unnecessary.
        // If weird things start to happen, add them back from original ResolveTransactionsFlow.

        val resolver = ZKTransactionsResolver(this)
        resolver.downloadDependencies(batchMode)

        logger.trace { "ResolveTransactionsFlow: Sending END." }
        otherSide.send(FetchZKDataFlow.Request.End) // Finish fetching data.

        // In ZKP mode we don't need to record any states although we still need to save ZKP backchain.
        resolver.recordDependencies(StatesToRecord.NONE)
    }
}
