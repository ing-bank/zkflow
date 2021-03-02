package com.ing.zknotary.node.services

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import net.corda.core.DeleteForDJVM
import net.corda.core.DoNotImplement
import net.corda.core.concurrent.CordaFuture
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.DataFeed
import net.corda.core.serialization.SerializeAsToken
import rx.Observable

/**
 * Thread-safe storage of ZKProverTransaction.
 */
@DeleteForDJVM
@DoNotImplement
interface ZKTransactionStorage<T> : SerializeAsToken {

    /**
     * Return the transaction with the given [id], or null if no such transaction exists.
     */
    fun getTransaction(id: SecureHash): T?

    /**
     * Get a synchronous Observable of updates.  When observations are pushed to the Observer, the vault will already
     * incorporate the update.
     */
    val updates: Observable<T>

    /**
     * Returns all currently stored transactions and further fresh ones.
     */
    fun track(): DataFeed<List<T>, T>

    /**
     * Returns a future that completes with the transaction corresponding to [id] once it has been committed
     */
    fun trackTransaction(id: SecureHash): CordaFuture<T>
}

/**
 * Thread-safe storage of transactions.
 */
interface ZKWritableTransactionStorage<T> : ZKTransactionStorage<T> {
    /**
     * Add a new *verified* transaction to the store, or convert the existing unverified transaction into a verified one.
     * @param transaction The transaction to be recorded.
     * @return true if the transaction was recorded as a *new verified* transcation, false if the transaction already exists.
     */
    fun addTransaction(transaction: T): Boolean

    /**
     * Add a new *unverified* transaction to the store.
     */
    fun addUnverifiedTransaction(transaction: T)

    /**
     * Return the transaction with the given ID from the store, and a flag of whether it's verified. Returns null if no transaction with the
     * ID exists.
     */
    fun getTransactionInternal(id: SecureHash): Pair<T, Boolean>?

    /**
     * Returns a future that completes with the transaction corresponding to [id] once it has been committed. Do not com.ing.zknotary.generator.warn when run inside
     * a DB transaction.
     */
    fun trackTransactionWithNoWarning(id: SecureHash): CordaFuture<T>
}

/**
 * Thread-safe storage of ZKVerifierTransaction.
 */
@DeleteForDJVM
@DoNotImplement
interface ZKVerifierTransactionStorage : ZKTransactionStorage<SignedZKVerifierTransaction>

/**
 * Thread-safe storage of transactions.
 */
interface ZKWritableVerifierTransactionStorage : ZKWritableTransactionStorage<SignedZKVerifierTransaction>, ZKVerifierTransactionStorage
