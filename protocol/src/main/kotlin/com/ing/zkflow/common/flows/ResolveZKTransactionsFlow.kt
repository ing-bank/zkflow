/*
 * Source attribution:
 *
 * Some flows in this file are strongly based on their original non-ZKP counterpart (i.e. without the 'ZK' prefix in the class name) from Corda
 * itself, as defined in the package net.corda.core.flows (https://github.com/corda/corda).
 *
 * Ideally ZKFlow could have extended the Corda flows to add the ZKP checks only, and leave the rest of the behaviour intact.
 * Unfortunately, Corda's flows were not implemented with extension in mind, and it was not possible to create this flow without copying most
 * of the original flow.
 */
package com.ing.zkflow.common.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.client.flows.FetchZKDataFlow
import com.ing.zkflow.common.transactions.fetchMissingAttachments
import net.corda.core.DeleteForDJVM
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.internal.PlatformVersionSwitches
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.CoreTransaction
import net.corda.core.utilities.debug
import net.corda.core.utilities.trace

/**
 * Resolves transactions for the specified [txHashes] along with their full history (dependency graph) from [otherSide].
 * Each retrieved transaction is validated and inserted into the local transaction storage.
 */
@DeleteForDJVM
class ResolveZKTransactionsFlow constructor(
    val initialTx: CoreTransaction? = null,
    val txHashes: Set<SecureHash>,
    val otherSide: FlowSession
) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        // TODO This error should actually cause the flow to be sent to the flow hospital to be retried
        val counterpartyPlatformVersion =
            checkNotNull(serviceHub.networkMapCache.getNodeByLegalIdentity(otherSide.counterparty)?.platformVersion) {
                "Couldn't retrieve party's ${otherSide.counterparty} platform version from NetworkMapCache"
            }

        val batchMode = counterpartyPlatformVersion >= PlatformVersionSwitches.BATCH_DOWNLOAD_COUNTERPARTY_BACKCHAIN
        logger.debug { "ResolveZKTransactionsFlow.call(): Otherside Platform Version = '$counterpartyPlatformVersion': Batch mode = $batchMode" }

        initialTx?. let { fetchMissingAttachments(it, otherSide) }

        val resolver = ZKTransactionsResolver(this)
        resolver.downloadDependencies(batchMode)

        logger.trace { "ResolveTransactionsFlow: Sending END." }
        otherSide.send(FetchZKDataFlow.Request.End) // Finish fetching data.

        // In ZKP mode we don't need to record any states although we still need to save ZKP backchain.
        resolver.recordDependencies(StatesToRecord.NONE)
    }

    fun fetchMissingAttachments(tx: CoreTransaction): Boolean = fetchMissingAttachments(tx, otherSide)
}
