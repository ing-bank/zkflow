package com.ing.zknotary.node.services

import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.transactions.toZKProverTransaction
import com.ing.zknotary.common.transactions.toZKVerifierTransaction
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZKTransactionService
import net.corda.core.DeleteForDJVM
import net.corda.core.DoNotImplement
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.DataFeed
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import rx.Observable

fun SignedTransaction.toZKVerifierTransaction(
    services: ServiceHub,
    zkStorage: ZKWritableTransactionStorage,
    zkTransactionService: ZKTransactionService,
    persist: Boolean = true
): ZKVerifierTransaction {
    val wtx = coreTransaction as WireTransaction
    val ptx = wtx.toZKProverTransaction(
        services,
        zkStorage,
        // TODO: make this configurable
        componentGroupLeafDigestService = BLAKE2s256DigestService,
        nodeDigestService = PedersenDigestService
    )

    fun List<PaddingWrapper<StateAndRef<ContractState>>>.collectUtxoNonces() = map {
        when (it) {
            is PaddingWrapper.Filler -> {
                // When it is a padded state, the nonce is ALWAYS a zerohash
                ptx.componentGroupLeafDigestService.zeroHash
            }
            is PaddingWrapper.Original -> {
                val outputTx =
                    zkStorage.getTransaction(it.content.ref.txhash)
                        ?: error("Could not fetch output transaction for StateRef ${it.content.ref}")
                outputTx.componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]!![it.content.ref.index]
            }
        }
    }

    // Collect the nonces for the outputs pointed to by the inputs and references.
    val inputNonces = ptx.padded.inputs().collectUtxoNonces()
    val referenceNonces = ptx.padded.references().collectUtxoNonces()

    val proof = zkTransactionService.prove(Witness(ptx, inputNonces = inputNonces, referenceNonces = referenceNonces))
    val vtx = ptx.toZKVerifierTransaction(proof)

    if (persist) {
        zkStorage.map.put(this, vtx)
        zkStorage.addTransaction(vtx)
    }

    return vtx
}

/**
 * Map from SignedTransaction.id to ZKProverTransaction.id for later lookup
 */
interface ZKTransactionMap {
    fun get(id: SecureHash): SecureHash?
    fun put(stx: SignedTransaction, vtx: ZKVerifierTransaction): SecureHash?
    fun get(stx: SignedTransaction): SecureHash?
    fun put(wtx: WireTransaction, ptx: ZKProverTransaction): SecureHash?
    fun get(wtx: WireTransaction): SecureHash?
}

/**
 * Thread-safe storage of ZKP transactions.
 */
@DeleteForDJVM
@DoNotImplement
interface ZKTransactionStorage : SerializeAsToken {
    val map: ZKTransactionMap

    /**
     * Return the transaction with the given [id], or null if no such transaction exists.
     */
    fun getTransaction(id: SecureHash): ZKVerifierTransaction?

    /**
     * Get a synchronous Observable of updates.  When observations are pushed to the Observer, the vault will already
     * incorporate the update.
     */
    val updates: Observable<ZKVerifierTransaction>

    /**
     * Returns all currently stored transactions and further fresh ones.
     */
    fun track(): DataFeed<List<ZKVerifierTransaction>, ZKVerifierTransaction>

    /**
     * Returns a future that completes with the transaction corresponding to [id] once it has been committed
     */
    fun trackTransaction(id: SecureHash): CordaFuture<ZKVerifierTransaction>

    fun zkVerifierTransactionFor(wtx: WireTransaction): ZKVerifierTransaction? {
        val id = map.get(wtx)
        return if (id != null) getTransaction(id) else null
    }
}

/**
 * Thread-safe storage of transactions.
 */
interface ZKWritableTransactionStorage : ZKTransactionStorage {
    /**
     * Add a new *verified* transaction to the store, or convert the existing unverified transaction into a verified one.
     * @param transaction The transaction to be recorded.
     * @return true if the transaction was recorded as a *new verified* transcation, false if the transaction already exists.
     */
    fun addTransaction(transaction: ZKVerifierTransaction): Boolean

    /**
     * Add a new *unverified* transaction to the store.
     */
    fun addUnverifiedTransaction(transaction: ZKVerifierTransaction)

    /**
     * Return the transaction with the given ID from the store, and a flag of whether it's verified. Returns null if no transaction with the
     * ID exists.
     */
    fun getTransactionInternal(id: SecureHash): Pair<ZKVerifierTransaction, Boolean>?

    /**
     * Returns a future that completes with the transaction corresponding to [id] once it has been committed. Do not warn when run inside
     * a DB transaction.
     */
    fun trackTransactionWithNoWarning(id: SecureHash): CordaFuture<ZKVerifierTransaction>
}