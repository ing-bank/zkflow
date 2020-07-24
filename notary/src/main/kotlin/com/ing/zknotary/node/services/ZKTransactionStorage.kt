package com.ing.zknotary.node.services

import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import net.corda.core.DeleteForDJVM
import net.corda.core.DoNotImplement
import net.corda.core.concurrent.CordaFuture
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.DataFeed
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import rx.Observable

/**
 * Map from SignedTransaction.id to ZKProverTransaction.id for later lookup
 */
interface ZKTransactionMap {
    fun get(id: SecureHash): SecureHash?
    fun put(stx: SignedTransaction, vtx: ZKVerifierTransaction): SecureHash?
    fun get(stx: SignedTransaction): SecureHash?
    fun put(wtx: WireTransaction, ptx: ZKProverTransaction): SecureHash?
    fun get(stx: WireTransaction): SecureHash?
}

/**
 * Thread-safe storage of ZKP transactions.
 */
@DeleteForDJVM
@DoNotImplement
interface ZKTransactionStorage {
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
