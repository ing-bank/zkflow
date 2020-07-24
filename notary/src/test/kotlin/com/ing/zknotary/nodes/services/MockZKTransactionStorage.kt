package com.ing.zknotary.nodes.services

import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.node.services.ZKTransactionMap
import com.ing.zknotary.node.services.ZKWritableTransactionStorage
import net.corda.core.concurrent.CordaFuture
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.concurrent.doneFuture
import net.corda.core.messaging.DataFeed
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.toFuture
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.node.services.api.WritableTransactionStorage
import net.corda.testing.node.MockServices
import rx.Observable
import rx.subjects.PublishSubject
import java.util.HashMap

/**
 * Map from SignedTransaction.id to ZKProverTransaction.id for later lookup
 */
open class MockZKTransactionMap : ZKTransactionMap, SingletonSerializeAsToken() {
    private val map = mutableMapOf<SecureHash, SecureHash>()

    override fun put(wtx: WireTransaction, ptx: ZKProverTransaction): SecureHash? = map.put(wtx.id, ptx.id)
    override fun put(stx: SignedTransaction, vtx: ZKVerifierTransaction): SecureHash? = map.put(stx.id, vtx.id)

    override fun get(id: SecureHash): SecureHash? = map[id]
    override fun get(stx: SignedTransaction): SecureHash? = map[stx.id]
    override fun get(stx: WireTransaction): SecureHash? = map[stx.id]
}

/**
 * A class which provides an implementation of [WritableTransactionStorage] which is used in [MockServices]
 */
@CordaService
open class MockZKTransactionStorage(val serviceHub: AppServiceHub) : ZKWritableTransactionStorage, SingletonSerializeAsToken() {

    override fun trackTransaction(id: SecureHash): CordaFuture<ZKVerifierTransaction> {
        return getTransaction(id)?.let { doneFuture(it) } ?: _updatesPublisher.filter { it.id == id }.toFuture()
    }

    override fun trackTransactionWithNoWarning(id: SecureHash): CordaFuture<ZKVerifierTransaction> {
        return trackTransaction(id)
    }

    override fun track(): DataFeed<List<ZKVerifierTransaction>, ZKVerifierTransaction> {
        return DataFeed(txns.values.mapNotNull { if (it.isVerified) it.vtx else null }, _updatesPublisher)
    }

    override val map: ZKTransactionMap = MockZKTransactionMap()

    private val txns = HashMap<SecureHash, TxHolder>()

    private val _updatesPublisher = PublishSubject.create<ZKVerifierTransaction>()

    override val updates: Observable<ZKVerifierTransaction>
        get() = _updatesPublisher

    private fun notify(transaction: ZKVerifierTransaction): Boolean {
        _updatesPublisher.onNext(transaction)
        return true
    }

    override fun addTransaction(transaction: ZKVerifierTransaction): Boolean {
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

    override fun addUnverifiedTransaction(transaction: ZKVerifierTransaction) {
        txns.putIfAbsent(
            transaction.id,
            TxHolder(
                transaction,
                isVerified = false
            )
        )
    }

    override fun getTransaction(id: SecureHash): ZKVerifierTransaction? =
        txns[id]?.let { if (it.isVerified) it.vtx else null }

    override fun getTransactionInternal(id: SecureHash): Pair<ZKVerifierTransaction, Boolean>? =
        txns[id]?.let { Pair(it.vtx, it.isVerified) }

    private class TxHolder(val vtx: ZKVerifierTransaction, var isVerified: Boolean)
}
