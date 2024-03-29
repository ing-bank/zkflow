package com.ing.zkflow.common.node.services

import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import net.corda.core.concurrent.CordaFuture
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.concurrent.doneFuture
import net.corda.core.messaging.DataFeed
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.toFuture
import rx.Observable
import rx.subjects.PublishSubject

/**
 * A class which provides an implementation of [WritableTransactionStorage] which is used in [MockServices]
 */
@CordaService
class InMemoryZKVerifierTransactionStorageCordaService(@Suppress("UNUSED_PARAMETER") services: AppServiceHub) :
    InMemoryZKVerifierTransactionStorage()

open class InMemoryZKVerifierTransactionStorage : ZKWritableVerifierTransactionStorage,
    SingletonSerializeAsToken() {

    override fun trackTransaction(id: SecureHash): CordaFuture<SignedZKVerifierTransaction> {
        return getTransaction(id)?.let { doneFuture(it) } ?: _updatesPublisher.filter { it.id == id }.toFuture()
    }

    override fun trackTransactionWithNoWarning(id: SecureHash): CordaFuture<SignedZKVerifierTransaction> {
        return trackTransaction(id)
    }

    override fun track(): DataFeed<List<SignedZKVerifierTransaction>, SignedZKVerifierTransaction> {
        return DataFeed(txns.values.mapNotNull { if (it.isVerified) it.vtx else null }, _updatesPublisher)
    }

    private val txns = HashMap<SecureHash, TxHolder>()

    private val _updatesPublisher = PublishSubject.create<SignedZKVerifierTransaction>()

    override val updates: Observable<SignedZKVerifierTransaction>
        get() = _updatesPublisher

    private fun notify(transaction: SignedZKVerifierTransaction): Boolean {
        _updatesPublisher.onNext(transaction)
        return true
    }

    override fun addTransaction(transaction: SignedZKVerifierTransaction): Boolean {
        val current = txns.putIfAbsent(
            transaction.id,
            TxHolder(
                transaction,
                isVerified = true
            )
        )
        return if (current == null) {
            notify(transaction)
        } else if (!current.isVerified) {
            current.isVerified = true
            notify(transaction)
        } else {
            false
        }
    }

    override fun addUnverifiedTransaction(transaction: SignedZKVerifierTransaction) {
        txns.putIfAbsent(
            transaction.id,
            TxHolder(
                transaction,
                isVerified = false
            )
        )
    }

    override fun getTransaction(id: SecureHash): SignedZKVerifierTransaction? =
        txns[id]?.let { if (it.isVerified) it.vtx else null }

    override fun getTransactionInternal(id: SecureHash): Pair<SignedZKVerifierTransaction, Boolean>? =
        txns[id]?.let { Pair(it.vtx, it.isVerified) }

    private class TxHolder(val vtx: SignedZKVerifierTransaction, var isVerified: Boolean)
}
