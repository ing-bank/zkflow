package com.ing.zknotary.node.services

import com.ing.zknotary.common.transactions.NamedByZKMerkleTree
import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.transactions.toWitness
import com.ing.zknotary.common.transactions.toZKProverTransaction
import com.ing.zknotary.common.transactions.toZKVerifierTransaction
import com.ing.zknotary.common.zkp.ZKTransactionService
import net.corda.core.DeleteForDJVM
import net.corda.core.DoNotImplement
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.NamedByHash
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
    zkProverTransactionStorage: ZKWritableProverTransactionStorage,
    zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage,
    zkTransactionService: ZKTransactionService,
    persist: Boolean = true
): ZKVerifierTransaction {
    val wtx = coreTransaction as WireTransaction
    val witness = wtx.toZKProverTransaction(
        services,
        zkProverTransactionStorage,
        componentGroupLeafDigestService = BLAKE2s256DigestService,
        nodeDigestService = PedersenDigestService
    ).toWitness(zkProverTransactionStorage)

    val proof = zkTransactionService.prove(witness)
    val vtx = witness.transaction.toZKVerifierTransaction(proof)

    if (persist) {
        zkProverTransactionStorage.map.put(this, vtx)
        zkVerifierTransactionStorage.map.put(this, vtx)
        zkProverTransactionStorage.addTransaction(witness.transaction)
        zkVerifierTransactionStorage.addTransaction(vtx)
    }

    return vtx
}

/**
 * Map from Standard Corda transactions id to ZKP transaction id
 */
interface ZKTransactionMap {
    fun get(id: SecureHash): SecureHash?
    fun get(tx: NamedByHash): SecureHash?
    fun put(tx: NamedByHash, zktx: NamedByZKMerkleTree): SecureHash?
}

/**
 * Thread-safe storage of ZKProverTransaction.
 */
@DeleteForDJVM
@DoNotImplement
interface ZKProverTransactionStorage : SerializeAsToken {
    val map: ZKTransactionMap

    /**
     * Return the transaction with the given [id], or null if no such transaction exists.
     */
    fun getTransaction(id: SecureHash): ZKProverTransaction?

    /**
     * Get a synchronous Observable of updates.  When observations are pushed to the Observer, the vault will already
     * incorporate the update.
     */
    val updates: Observable<ZKProverTransaction>

    /**
     * Returns all currently stored transactions and further fresh ones.
     */
    fun track(): DataFeed<List<ZKProverTransaction>, ZKProverTransaction>

    /**
     * Returns a future that completes with the transaction corresponding to [id] once it has been committed
     */
    fun trackTransaction(id: SecureHash): CordaFuture<ZKProverTransaction>

    fun zkTransactionFor(tx: NamedByHash): ZKProverTransaction? = map.get(tx)?.let { getTransaction(it) }
}

/**
 * Thread-safe storage of transactions.
 */
interface ZKWritableProverTransactionStorage : ZKProverTransactionStorage {
    /**
     * Add a new *verified* transaction to the store, or convert the existing unverified transaction into a verified one.
     * @param transaction The transaction to be recorded.
     * @return true if the transaction was recorded as a *new verified* transcation, false if the transaction already exists.
     */
    fun addTransaction(transaction: ZKProverTransaction): Boolean

    /**
     * Add a new *unverified* transaction to the store.
     */
    fun addUnverifiedTransaction(transaction: ZKProverTransaction)

    /**
     * Return the transaction with the given ID from the store, and a flag of whether it's verified. Returns null if no transaction with the
     * ID exists.
     */
    fun getTransactionInternal(id: SecureHash): Pair<ZKProverTransaction, Boolean>?

    /**
     * Returns a future that completes with the transaction corresponding to [id] once it has been committed. Do not warn when run inside
     * a DB transaction.
     */
    fun trackTransactionWithNoWarning(id: SecureHash): CordaFuture<ZKProverTransaction>
}

/**
 * Thread-safe storage of ZKVerifierTransaction.
 */
@DeleteForDJVM
@DoNotImplement
interface ZKVerifierTransactionStorage : SerializeAsToken {
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

    fun zkTransactionFor(tx: NamedByHash): ZKVerifierTransaction? = map.get(tx)?.let { getTransaction(it) }
}

/**
 * Thread-safe storage of transactions.
 */
interface ZKWritableVerifierTransactionStorage : ZKVerifierTransactionStorage {
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
