package com.ing.zknotary.nodes.services

import com.ing.zknotary.common.transactions.NamedByZKMerkleTree
import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.node.services.ZKTransactionMap
import com.ing.zknotary.node.services.ZKWritableProverTransactionStorage
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.NamedByHash
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.concurrent.doneFuture
import net.corda.core.messaging.DataFeed
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.toFuture
import net.corda.node.services.api.WritableTransactionStorage
import net.corda.testing.node.MockServices
import rx.Observable
import rx.subjects.PublishSubject
import java.util.HashMap

/**
 * A class which provides an implementation of [WritableTransactionStorage] which is used in [MockServices]
 */
@CordaService
open class MockZKProverTransactionStorage(val serviceHub: AppServiceHub) : ZKWritableProverTransactionStorage,
    SingletonSerializeAsToken() {

    override fun trackTransaction(id: SecureHash): CordaFuture<ZKProverTransaction> {
        return getTransaction(id)?.let { doneFuture(it) } ?: _updatesPublisher.filter { it.id == id }.toFuture()
    }

    override fun trackTransactionWithNoWarning(id: SecureHash): CordaFuture<ZKProverTransaction> {
        return trackTransaction(id)
    }

    override fun track(): DataFeed<List<ZKProverTransaction>, ZKProverTransaction> {
        return DataFeed(txns.values.mapNotNull { if (it.isVerified) it.vtx else null }, _updatesPublisher)
    }

    override val map: ZKTransactionMap = MockZKTransactionMap()

    private val txns = HashMap<SecureHash, TxHolder>()

    private val _updatesPublisher = PublishSubject.create<ZKProverTransaction>()

    override val updates: Observable<ZKProverTransaction>
        get() = _updatesPublisher

    private fun notify(transaction: ZKProverTransaction): Boolean {
        _updatesPublisher.onNext(transaction)
        return true
    }

    override fun addTransaction(transaction: ZKProverTransaction): Boolean {
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

    override fun addUnverifiedTransaction(transaction: ZKProverTransaction) {
        txns.putIfAbsent(
            transaction.id,
            TxHolder(
                transaction,
                isVerified = false
            )
        )
    }

    override fun getTransaction(id: SecureHash): ZKProverTransaction? =
        txns[id]?.let { if (it.isVerified) it.vtx else null }

    override fun getTransactionInternal(id: SecureHash): Pair<ZKProverTransaction, Boolean>? =
        txns[id]?.let { Pair(it.vtx, it.isVerified) }

    private class TxHolder(val vtx: ZKProverTransaction, var isVerified: Boolean)
}

/**
 * A class which provides an implementation of [WritableTransactionStorage] which is used in [MockServices]
 */
@CordaService
open class MockZKVerifierTransactionStorage(val serviceHub: AppServiceHub) : ZKWritableVerifierTransactionStorage,
    SingletonSerializeAsToken() {

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

/**
 * Map from Standard Corda transactions id to ZKP transaction id
 */
private class MockZKTransactionMap : ZKTransactionMap, SingletonSerializeAsToken() {
    private val map = mutableMapOf<SecureHash, SecureHash>()

    override fun get(id: SecureHash) = map[id]
    override fun get(tx: NamedByHash) = map[tx.id]

    override fun put(tx: NamedByHash, zktx: NamedByZKMerkleTree) = map.put(tx.id, zktx.merkleTree.root)
}
