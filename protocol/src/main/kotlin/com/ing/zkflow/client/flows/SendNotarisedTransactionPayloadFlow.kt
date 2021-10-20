package com.ing.zkflow.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.transactions.NotarisedTransactionPayload
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.UtxoInfo
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import com.ing.zkflow.notary.ZKNotarisationPayload
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.internal.NetworkParametersStorage
import net.corda.core.internal.RetrieveAnyTransactionPayload
import net.corda.core.internal.readFully
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.utilities.trace
import net.corda.core.utilities.unwrap

/**
 * The [ZKSendTransactionProposal] should be used to send a transaction to another peer that wishes to verify that transaction's
 * integrity by resolving and checking the dependencies as well. The other side should invoke [ZKReceiveTransactionProposalFlow] at
 * the right point in the conversation to receive the sent transaction and perform the resolution back-and-forth required
 * to check the dependencies and download any missing attachments.
 *
 * @param otherSide the target party.
 * @param stx the [SignedTransaction] being sent to the [otherSideSession].
 */
open class ZKSendTransactionProposal(private val otherSideSession: FlowSession, private val stx: SignedTransaction) :
    FlowLogic<Void?>() {
    @Suspendable
    override fun call(): Void? {
        otherSideSession.send(stx)
        val utxoInfos = serviceHub.collectUtxoInfos(stx.inputs + stx.references)
        subFlow(SendUtxoInfosFlow(otherSideSession, utxoInfos))
        return null
    }
}

open class SendNotarisedTransactionPayloadFlow(otherSide: FlowSession, notarised: NotarisedTransactionPayload) :
    ZKDataVendingFlow(otherSide, notarised)

open class SendUtxoInfosFlow(otherSide: FlowSession, utxoInfos: List<UtxoInfo>) :
    ZKDataVendingFlow(otherSide, utxoInfos)

/**
 * The [ZKSendStateAndRefFlow] should be used to send a list of input [StateAndRef] to another peer that wishes to verify
 * the input's integrity by resolving and checking the dependencies as well. The other side should invoke [ZKReceiveStateAndRefFlow]
 * at the right point in the conversation to receive the input state and ref and perform the resolution back-and-forth
 * required to check the dependencies.
 *
 * @param otherSideSession the target session.
 * @param stateAndRefs the list of [StateAndRef] being sent to the [otherSideSession].
 */
class ZKSendStateAndRefFlow(
    private val otherSideSession: FlowSession,
    private val stateAndRefs: List<StateAndRef<*>>
) : FlowLogic<Void?>() {
    @Suspendable
    override fun call(): Void? {
        val utxoInfos = serviceHub.collectUtxoInfos(stateAndRefs.map { it.ref })
        subFlow(SendUtxoInfosFlow(otherSideSession, utxoInfos))
        return null
    }
}

open class ZKDataVendingFlow(val otherSideSession: FlowSession, val payload: Any) : FlowLogic<Void?>() {

    @Suspendable
    protected open fun sendPayloadAndReceiveDataRequest(otherSideSession: FlowSession, payload: Any) =
        otherSideSession.sendAndReceive<FetchZKDataFlow.Request>(payload)

    @Suspendable
    protected open fun verifyDataRequest(dataRequest: FetchZKDataFlow.Request.Data) {
        // User can override this method to perform custom request verification.
    }

    @Suppress("LongMethod", "ComplexMethod")
    @Suspendable
    override fun call(): Void? {
        val networkMaxMessageSize = serviceHub.networkParameters.maxMessageSize
        val maxPayloadSize = networkMaxMessageSize / 2

        logger.trace { "DataVendingFlow: Call: Network max message size = $networkMaxMessageSize, Max Payload Size = $maxPayloadSize" }

        val zkStorage =
            serviceHub.getCordaServiceFromConfig<ZKVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE)

        // The first payload will be the transaction data, subsequent payload will be the transaction/attachment/network parameters data.
        var payload = payload

        // Depending on who called this flow, the type of the initial payload is different.
        // The authorisation logic is to maintain a dynamic list of transactions that the caller is authorised to make based on the transactions that were made already.
        // Each time an authorised transaction is requested, the input transactions are added to the list.
        // Once a transaction has been requested, it will be removed from the authorised list. This means that it is a protocol violation to request a transaction twice.
        val authorisedTransactions = when (payload) {
            is ZKNotarisationPayload -> TransactionAuthorisationFilter().addAuthorised(getInputTransactions(payload.transaction.tx))
            is NotarisedTransactionPayload -> TransactionAuthorisationFilter().addAuthorised(
                getInputTransactions(
                    payload.stx.tx
                )
            )

            is RetrieveAnyTransactionPayload -> TransactionAuthorisationFilter(acceptAll = true)
            is List<*> -> TransactionAuthorisationFilter().addAuthorised(
                payload.flatMap { payloadItem ->
                    when (payloadItem) {
                        is StateAndRef<*> -> {
                            getInputTransactions(zkStorage.getTransaction(payloadItem.ref.txhash)!!.tx) + payloadItem.ref.txhash
                        }
                        is UtxoInfo -> {
                            getInputTransactions(zkStorage.getTransaction(payloadItem.stateRef.txhash)!!.tx) + payloadItem.stateRef.txhash
                        }
                        else -> {
                            throw IllegalArgumentException("Unknown payload type: ${payloadItem!!::class.java} ?")
                        }
                    }
                }.toSet()
            )
            else -> throw IllegalArgumentException("Unknown payload type: ${payload::class.java} ?")
        }

        // This loop will receive [FetchZKDataFlow.Request] continuously until the `otherSideSession` has all the data they need
        // to resolve the transaction, a [FetchZKDataFlow.EndRequest] will be sent from the `otherSideSession` to indicate end of
        // data request.
        var loopCount = 0
        while (true) {
            val loopCnt = loopCount++
            logger.trace { "DataVendingFlow: Main While [$loopCnt]..." }
            val dataRequest = sendPayloadAndReceiveDataRequest(otherSideSession, payload).unwrap { request ->
                logger.trace { "sendPayloadAndReceiveDataRequest(): ${request.javaClass.name}" }
                when (request) {
                    is FetchZKDataFlow.Request.Data -> {
                        // Security TODO: Check for abnormally large or malformed data requests
                        verifyDataRequest(request)
                        request
                    }
                    FetchZKDataFlow.Request.End -> {
                        logger.trace { "DataVendingFlow: END" }
                        return null
                    }
                }
            }

            logger.trace { "Sending data (Type = ${dataRequest.dataType.name})" }
            var totalByteCount = 0
            var firstItem = true
            var batchFetchCountExceeded = false
            var numSent = 0
            payload = when (dataRequest.dataType) {
                FetchZKDataFlow.DataType.TRANSACTION -> dataRequest.hashes.map { txId ->
                    logger.trace { "Sending: TRANSACTION (dataRequest.hashes.size=${dataRequest.hashes.size})" }
                    if (!authorisedTransactions.isAuthorised(txId)) {
                        throw FetchZKDataFlow.IllegalTransactionRequest(txId)
                    }
                    val tx = zkStorage.getTransaction(txId)
                        ?: throw FetchZKDataFlow.HashNotFound(txId)
                    authorisedTransactions.removeAuthorised(tx.id)
                    authorisedTransactions.addAuthorised(getInputTransactions(tx.tx))
                    val serialized = tx.serialize()
                    totalByteCount += serialized.size
                    numSent++
                    serialized
                }
                // Loop on all items returned using dataRequest.hashes.map:
                FetchZKDataFlow.DataType.BATCH_TRANSACTION -> dataRequest.hashes.map { txId ->
                    if (!authorisedTransactions.isAuthorised(txId)) {
                        throw FetchZKDataFlow.IllegalTransactionRequest(txId)
                    }
                    // Maybe we should not just throw here as it's not recoverable on the client side. Might be better to send a reason code or
                    // remove the restriction on sending once.
                    logger.trace { "Transaction authorised OK: '$txId'" }
                    var serialized: SerializedBytes<SignedZKVerifierTransaction>? = null
                    if (!batchFetchCountExceeded) {
                        // Only fetch and serialize if we have not already exceeded the maximum byte count. Once we have, no more fetching
                        // is required, just reject all additional items.
                        val tx = zkStorage.getTransaction(txId)
                            ?: throw FetchZKDataFlow.HashNotFound(txId)
                        logger.trace { "Transaction get OK: '$txId'" }
                        serialized = tx.serialize()

                        val itemByteCount = serialized.size
                        logger.trace { "Batch-Send '$txId': first = $firstItem, Total bytes = $totalByteCount, Item byte count = $itemByteCount, Maximum = $maxPayloadSize" }
                        if (firstItem || (totalByteCount + itemByteCount) < maxPayloadSize) {
                            totalByteCount += itemByteCount
                            numSent++
                            // Always include at least one item else if the max is set too low nothing will ever get returned.
                            // Splitting items will be a separate Jira if need be
                            authorisedTransactions.removeAuthorised(tx.id)
                            authorisedTransactions.addAuthorised(getInputTransactions(tx.tx))
                            logger.trace { "Adding item to return set: '$txId'" }
                        } else {
                            logger.trace { "Fetch block size EXCEEDED at '$txId'." }
                            batchFetchCountExceeded = true
                        }
                    } // end

                    if (batchFetchCountExceeded) {
                        logger.trace { "Excluding '$txId' from return set due to exceeded count." }
                    }

                    // Send null if limit is exceeded
                    val maybeserialized = MaybeSerializedSignedZKVerifierTransaction(
                        txId,
                        if (batchFetchCountExceeded) {
                            null
                        } else {
                            serialized
                        },
                        null
                    )
                    firstItem = false
                    maybeserialized
                } // Batch response loop end
                FetchZKDataFlow.DataType.ATTACHMENT -> dataRequest.hashes.map {
                    logger.trace { "Sending: Attachments for '$it'" }
                    serviceHub.attachments.openAttachment(it)?.open()?.readFully()
                        ?: throw FetchZKDataFlow.HashNotFound(it)
                }
                FetchZKDataFlow.DataType.PARAMETERS -> dataRequest.hashes.map {
                    logger.trace { "Sending: Parameters for '$it'" }
                    (serviceHub.networkParametersService as NetworkParametersStorage).lookupSigned(it)
                        ?: throw FetchZKDataFlow.MissingNetworkParameters(it)
                }
                FetchZKDataFlow.DataType.UNKNOWN -> dataRequest.hashes.map {
                    logger.warn("Message from from a future version of Corda with UNKNOWN enum value for FetchZKDataFlow.DataType: ID='$it'")
                }
            }
            logger.trace { "Block total size = $totalByteCount: Num Items = ($numSent of ${dataRequest.hashes.size} total)" }
        }
    }

    @Suspendable
    private fun getInputTransactions(tx: TraversableTransaction): Set<SecureHash> {
        return tx.inputs.map { it.txhash }.toSet() + tx.references.map { it.txhash }.toSet()
    }

    private class TransactionAuthorisationFilter(
        private val authorisedTransactions: MutableSet<SecureHash> = mutableSetOf(),
        val acceptAll: Boolean = false
    ) {
        fun isAuthorised(txId: SecureHash) = acceptAll || authorisedTransactions.contains(txId)

        fun addAuthorised(txs: Set<SecureHash>): TransactionAuthorisationFilter {
            authorisedTransactions.addAll(txs)
            return this
        }

        fun removeAuthorised(txId: SecureHash) {
            authorisedTransactions.remove(txId)
        }
    }
}
