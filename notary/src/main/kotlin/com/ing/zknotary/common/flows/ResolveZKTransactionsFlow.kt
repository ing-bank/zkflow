package com.ing.zknotary.common.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.node.services.ZKTransactionsResolver
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
        val counterpartyPlatformVersion = checkNotNull(serviceHub.networkMapCache.getNodeByLegalIdentity(otherSide.counterparty)?.platformVersion) {
            "Couldn't retrieve party's ${otherSide.counterparty} platform version from NetworkMapCache"
        }

        val batchMode = counterpartyPlatformVersion >= PlatformVersionSwitches.BATCH_DOWNLOAD_COUNTERPARTY_BACKCHAIN
        logger.debug { "ResolveTransactionsFlow.call(): Otherside Platform Version = '$counterpartyPlatformVersion': Batch mode = $batchMode" }

        fetchMissingAttachments(initialTx)
// TODO        fetchMissingNetworkParameters(initialTx)

        val resolver = ZKTransactionsResolver(this)
        resolver.downloadDependencies(batchMode)

        logger.trace { "ResolveTransactionsFlow: Sending END." }
        otherSide.send(FetchZKDataFlow.Request.End) // Finish fetching data.

        // In ZKP mode we don't need to record any states although we still need to save ZKP backchain.
        resolver.recordDependencies(StatesToRecord.NONE)
    }

    /**
     * Fetches the set of attachments required to verify the given transaction. If these are not already present, they will be fetched from
     * a remote peer.
     *
     * @param transaction The transaction to fetch attachments for
     * @return True if any attachments were fetched from a remote peer, false otherwise
     */
    // TODO: This could be done in parallel with other fetches for extra speed.
    @Suspendable
    fun fetchMissingAttachments(transaction: SignedTransaction?): Boolean {
        // TODO attachments are not supported yet
        return false
    }

    /**
     * Fetches the network parameters under which the given transaction was created. Note that if the transaction was created pre-V4, or if
     * the counterparty does not understand that network parameters may need to be fetched, no parameters will be requested.
     *
     * @param transaction The transaction to fetch the network parameters for, if the parameters are not already present
     * @return True if the network parameters were fetched from a remote peer, false otherwise
     */
    // TODO This can also be done in parallel. See comment to [fetchMissingAttachments] above.
    @Suspendable
    fun fetchMissingNetworkParameters(transaction: SignedZKVerifierTransaction): Boolean {
        // TODO network parameters are not supported yet
        return false
    }
}
