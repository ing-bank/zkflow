package com.ing.zkflow.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.client.flows.FetchZKDataFlow.DownloadedVsRequestedDataMismatch
import com.ing.zkflow.client.flows.FetchZKDataFlow.HashNotFound
import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import net.corda.core.contracts.NamedByHash
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.internal.uncheckedCast
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.CordaSerializationTransformEnumDefault
import net.corda.core.serialization.CordaSerializationTransformEnumDefaults
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.utilities.NonEmptySet
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.debug
import net.corda.core.utilities.trace
import net.corda.core.utilities.unwrap

/**
 * An abstract flow for fetching typed data from a remote peer.
 *
 * Given a set of hashes (IDs), either loads them from local disk or asks the remote peer to provide them.
 *
 * A malicious response in which the data provided by the remote peer does not hash to the requested hash results in
 * [DownloadedVsRequestedDataMismatch] being thrown. If the remote peer doesn't have an entry, it results in a
 * [HashNotFound] exception being thrown.
 *
 * By default this class does not insert data into any local database, if you want to do that after missing items were
 * fetched then override [maybeWriteToDisk]. You *must* override [load]. If the wire type is not the same as the
 * ultimate type, you must also override [convert].
 *
 * @param T The ultimate type of the data being fetched.
 * @param W The wire type of the data being fetched, for when it isn't the same as the ultimate type.
 */

sealed class FetchZKDataFlow<T : NamedByHash, in W : Any>(
    protected val requests: Set<SecureHash>,
    protected val otherSideSession: FlowSession,
    protected val dataType: DataType
) : FlowLogic<FetchZKDataFlow.Result<T>>() {

    @CordaSerializable
    class DownloadedVsRequestedDataMismatch(val requested: SecureHash, val got: SecureHash) : IllegalArgumentException()

    @CordaSerializable
    class DownloadedVsRequestedSizeMismatch(val requested: Int, val got: Int) : IllegalArgumentException()

    class HashNotFound(val requested: SecureHash) : FlowException()

    class MissingNetworkParameters(val requested: SecureHash) : FlowException("Failed to fetch network parameters with hash: $requested")

    class IllegalTransactionRequest(val requested: SecureHash) : FlowException(
        "Illegal attempt to request a transaction ($requested)" +
            " that is not in the transitive dependency graph of the sent transaction."
    )

    @CordaSerializable
    data class Result<out T : NamedByHash>(val fromDisk: List<T>, val downloaded: List<T>)

    @CordaSerializable
    sealed class Request {
        data class Data(val hashes: NonEmptySet<SecureHash>, val dataType: DataType) : Request()
        object End : Request()
    }

    // https://docs.corda.net/serialization-enum-evolution.html
    // Below annotations added to map two new enum values (BATCH_TRANSACTION and UNKNOWN) onto  TRANSACTION. The effect of this is that
    // if a that does not have these enum values receives it will not throw an error during deserialization. The purpose of adding
    // UNKNOWN is such that future additions can default to UNKNOWN rather than an existing value. In this instance we are protecting
    // against not having unknown by using the platform version as a guard.
    @CordaSerializationTransformEnumDefaults(
        CordaSerializationTransformEnumDefault("BATCH_TRANSACTION", "TRANSACTION"),
        CordaSerializationTransformEnumDefault("UNKNOWN", "TRANSACTION")
    )
    @CordaSerializable
    enum class DataType {
        TRANSACTION, ATTACHMENT, PARAMETERS, BATCH_TRANSACTION, UNKNOWN
    }

    @Suspendable
    @Throws(HashNotFound::class, MissingNetworkParameters::class)
    override fun call(): Result<T> {
        // Load the items we have from disk and figure out which we're missing.
        val (fromDisk, toFetch) = loadWhatWeHave()

        return if (toFetch.isEmpty()) {
            logger.trace { "FetchDataFlow.call(): loadWhatWeHave(): From disk size = ${fromDisk.size}: No items to fetch." }
            val loadedFromDisk = loadExpected(fromDisk)
            Result(loadedFromDisk, emptyList())
        } else {
            logger.trace { "FetchDataFlow.call(): loadWhatWeHave(): From disk size = ${fromDisk.size}, To-fetch size = ${toFetch.size}" }
            logger.debug { "Requesting ${toFetch.size} dependency(s) for verification from ${otherSideSession.counterparty.name}" }

            // TODO: Support "large message" response streaming so response sizes are not limited by RAM.
            // We can then switch to requesting items in large batches to minimise the latency penalty.
            // This is blocked by bugs ARTEMIS-1278 and ARTEMIS-1279. For now we limit attachments and txns to 10mb each
            // and don't request items in batch, which is a performance loss, but works around the issue. We have
            // configured Artemis to not fragment messages up to 10mb so we can send 10mb messages without problems.
            // Above that, we start losing authentication data on the message fragments and take exceptions in the
            // network layer.
            val maybeItems = ArrayList<W>()
            if (toFetch.size == 1) {
                val hash = toFetch.single()
                // We skip the validation here (with unwrap { it }) because we will do it below in validateFetchResponse.
                // The only thing checked is the object type.
                // TODO We need to page here after large messages will work.
                logger.trace { "[Single fetch]: otherSideSession.sendAndReceive($hash): Fetch type: ${dataType.name}" }
                // should only pass single item dataType below.
                maybeItems += otherSideSession.sendAndReceive<List<W>>(Request.Data(NonEmptySet.of(hash), dataType)).unwrap { it }
            } else {
                logger.trace { "[Batch fetch]: otherSideSession.sendAndReceive(set of ${toFetch.size}): Fetch type: ${dataType.name})" }
                maybeItems += otherSideSession.sendAndReceive<List<W>>(Request.Data(NonEmptySet.copyOf(toFetch), dataType))
                    .unwrap { it }
                logger.trace { "[Batch fetch]: otherSideSession.sendAndReceive Done: count= ${maybeItems.size})" }
            }

            // Check for a buggy/malicious peer answering with something that we didn't ask for.
            val downloaded = validateFetchResponse(UntrustworthyData(maybeItems), toFetch)
            logger.trace { "Fetched ${downloaded.size} elements from ${otherSideSession.counterparty.name}, maybeItems.size = ${maybeItems.size}" }
            maybeWriteToDisk(downloaded)

            // Re-load items already present before the download procedure. This ensures these objects are not unnecessarily checkpointed.
            val loadedFromDisk = loadExpected(fromDisk)
            Result(loadedFromDisk, downloaded)
        }
    }

    protected open fun maybeWriteToDisk(downloaded: List<T>) {
        // Do nothing by default.
    }

    private fun loadWhatWeHave(): Pair<List<SecureHash>, Set<SecureHash>> {
        val fromDisk = ArrayList<SecureHash>()
        val toFetch = LinkedHashSet<SecureHash>()
        for (txid in requests) {
            val stx = load(txid)
            if (stx == null)
                toFetch += txid
            else
            // Although the full object is loaded here, only return the id. This prevents the full set of objects already present from
            // being checkpointed every time a request is made to download an object the node does not yet have.
                fromDisk += txid
        }
        return Pair(fromDisk, toFetch)
    }

    private fun loadExpected(ids: List<SecureHash>): List<T> {
        val loaded = ids.mapNotNull { load(it) }
        require(ids.size == loaded.size) {
            "Expected to find ${ids.size} items in database but only found ${loaded.size} items"
        }
        return loaded
    }

    protected abstract fun load(txid: SecureHash): T?

    protected open fun convert(wire: W): T = uncheckedCast(wire)

    @Suppress("ComplexMethod")
    private fun validateFetchResponse(
        maybeItems: UntrustworthyData<ArrayList<W>>,
        requests: Set<SecureHash>
    ): List<T> {
        return maybeItems.unwrap { response ->
            logger.trace { "validateFetchResponse(): Response size = ${response.size}, Request size = ${requests.size}" }
            if (response.size != requests.size) {
                logger.trace { "maybeItems.unwrap: RespType Response.size (${requests.size}) != requests.size (${response.size})" }
                throw DownloadedVsRequestedSizeMismatch(requests.size, response.size)
            }

            if (logger.isTraceEnabled()) {
                logger.trace { "Request size = ${requests.size}" }
                for ((reqInd, req) in requests.withIndex()) {
                    logger.trace { "Requested[$reqInd] = '$req'" }
                }
            }

            val answers = response.map { convert(it) }
            if (logger.isTraceEnabled()) {
                logger.trace { "Answers size = ${answers.size}" }
                for ((respInd, item) in answers.withIndex()) {
                    if (item is MaybeSerializedSignedZKVerifierTransaction) {
                        logger.trace { "ValidateItem[$respInd]: '${item.id}': Type = MaybeSerializedSignedZKVerifierTransaction: ${item.payloadContentDescription()}" }
                    } else {
                        logger.trace("ValidateItem[$respInd]: Type = ${item.javaClass.name}")
                    }
                }
            }

            // Check transactions actually hash to what we requested, if this fails the remote node
            // is a malicious flow violator or buggy.
            var badDataIndex = -1
            var badDataId: SecureHash? = null
            for ((index, item) in requests.withIndex()) {
                if (item != answers[index].id) {
                    badDataIndex = index
                    badDataId = item
                    logger.info("Will Throw on DownloadedVsRequestedDataMismatch(Req item = '$item', Resp item = '${answers[index].id}'")
                }
            }

            if (badDataIndex >= 0 && badDataId != null) {
                logger.error("Throwing DownloadedVsRequestedDataMismatch due to bad verification on: ID = $badDataId, Answer[$badDataIndex]='${answers[badDataIndex].id}'")
                throw DownloadedVsRequestedDataMismatch(badDataId, answers[badDataIndex].id)
            }

            answers
        }
    }
}

/**
 * Given a set of tx hashes (IDs), either loads them from local disk or asks the remote peer to provide them.
 *
 * A malicious response in which the data provided by the remote peer does not hash to the requested hash results in
 * [FetchDataFlow.DownloadedVsRequestedDataMismatch] being thrown.
 * If the remote peer doesn't have an entry, it results in a [FetchDataFlow.HashNotFound] exception.
 * If the remote peer is not authorized to request this transaction, it results in a [FetchDataFlow.IllegalTransactionRequest] exception.
 * Authorisation is accorded only on valid ancestors of the root transaction.
 * Note that returned transactions are not inserted into the database, because it's up to the caller to actually verify the transactions are valid.
 */
class FetchZKTransactionsFlow(requests: Set<SecureHash>, otherSide: FlowSession) :
    FetchZKDataFlow<SignedZKVerifierTransaction, SerializedBytes<SignedZKVerifierTransaction>>(requests, otherSide, DataType.TRANSACTION) {

    override fun load(txid: SecureHash): SignedZKVerifierTransaction? = serviceHub.getCordaServiceFromConfig<ZKVerifierTransactionStorage>(
        ServiceNames.ZK_VERIFIER_TX_STORAGE
    ).getTransaction(txid)

    override fun convert(wire: SerializedBytes<SignedZKVerifierTransaction>): SignedZKVerifierTransaction = wire.deserialize()
}

class FetchBatchZKTransactionsFlow(requests: Set<SecureHash>, otherSide: FlowSession) :
    FetchZKDataFlow<MaybeSerializedSignedZKVerifierTransaction, MaybeSerializedSignedZKVerifierTransaction>(
        requests, otherSide,
        DataType.BATCH_TRANSACTION
    ) {

    override fun load(txid: SecureHash): MaybeSerializedSignedZKVerifierTransaction? {
        val tran =
            serviceHub.getCordaServiceFromConfig<ZKVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE).getTransaction(txid)
        return if (tran == null) {
            null
        } else {
            MaybeSerializedSignedZKVerifierTransaction(txid, null, tran)
        }
    }
}

/**
 * In the words of Matt working code is more important then pretty code. This class that contains code that may
 * be serialized. If it were always serialized then the local disk fetch would need to serialize then de-serialize
 * which wastes time. However over the wire we get batch fetch items serialized. This is because we need to get the exact
 * length of the objects to pack them into the 10MB max message size buffer. We do not want to serialize them multiple times
 * so it's a lot more efficient to send the byte stream.
 */
@CordaSerializable
class MaybeSerializedSignedZKVerifierTransaction(
    override val id: SecureHash,
    val serialized: SerializedBytes<SignedZKVerifierTransaction>?,
    val nonSerialised: SignedZKVerifierTransaction?
) : NamedByHash {
    init {
        check(serialized == null || nonSerialised == null) {
            "MaybeSerializedSignedTransaction: Serialized and non-serialized may not both be non-null."
        }
    }

    fun get(): SignedZKVerifierTransaction? {
        return if (nonSerialised != null) {
            nonSerialised
        } else if (serialized != null) {
            val tranBytes = SerializedBytes<SignedZKVerifierTransaction>(serialized.bytes)
            tranBytes.deserialize()
        } else {
            null
        }
    }

    fun isNull(): Boolean {
        return serialized == null && nonSerialised == null
    }

    fun serializedByteCount(): Int {
        return serialized?.bytes?.size ?: 0
    }

    fun payloadContentDescription(): String {
        val tranSize = serializedByteCount()
        val isSer = serialized != null
        val isObj = nonSerialised != null
        return if (isNull()) {
            "<Null>"
        } else "size = $tranSize, serialized = $isSer, isObj = $isObj"
    }
}
