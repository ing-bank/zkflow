package com.ing.zknotary.testing.node.services

import com.ing.zknotary.common.transactions.NamedByZKMerkleTree
import com.ing.zknotary.common.transactions.SignedZKProverTransaction
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
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
public open class MockZKProverTransactionStorage(public val serviceHub: AppServiceHub) : ZKWritableProverTransactionStorage,
    SingletonSerializeAsToken() {

    override fun trackTransaction(id: SecureHash): CordaFuture<SignedZKProverTransaction> {
        return getTransaction(id)?.let { doneFuture(it) } ?: _updatesPublisher.filter { it.id == id }.toFuture()
    }

    override fun trackTransactionWithNoWarning(id: SecureHash): CordaFuture<SignedZKProverTransaction> {
        return trackTransaction(id)
    }

    override fun track(): DataFeed<List<SignedZKProverTransaction>, SignedZKProverTransaction> {
        return DataFeed(txns.values.mapNotNull { if (it.isVerified) it.vtx else null }, _updatesPublisher)
    }

    override val map: ZKTransactionMap = MockZKTransactionMap()

    private val txns = HashMap<SecureHash, TxHolder>()

    private val _updatesPublisher = PublishSubject.create<SignedZKProverTransaction>()

    override val updates: Observable<SignedZKProverTransaction>
        get() = _updatesPublisher

    private fun notify(transaction: SignedZKProverTransaction): Boolean {
        _updatesPublisher.onNext(transaction)
        return true
    }

    override fun addTransaction(transaction: SignedZKProverTransaction): Boolean {
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

    override fun addUnverifiedTransaction(transaction: SignedZKProverTransaction) {
        txns.putIfAbsent(
            transaction.id,
            TxHolder(
                transaction,
                isVerified = false
            )
        )
    }

    override fun getTransaction(id: SecureHash): SignedZKProverTransaction? =
        txns[id]?.let { if (it.isVerified) it.vtx else null }

    override fun getTransactionInternal(id: SecureHash): Pair<SignedZKProverTransaction, Boolean>? =
        txns[id]?.let { Pair(it.vtx, it.isVerified) }

    private class TxHolder(val vtx: SignedZKProverTransaction, var isVerified: Boolean)
}

/**
 * A class which provides an implementation of [WritableTransactionStorage] which is used in [MockServices]
 */
@CordaService
public open class MockZKVerifierTransactionStorage(public val serviceHub: AppServiceHub) : ZKWritableVerifierTransactionStorage,
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

    override val map: ZKTransactionMap = MockZKTransactionMap()

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

/**
 * Map from Standard Corda transactions id to ZKP transaction id
 */
private class MockZKTransactionMap : ZKTransactionMap, SingletonSerializeAsToken() {
    private val map = mutableMapOf<SecureHash, SecureHash>()

    override fun get(id: SecureHash) = map[id]
    override fun get(tx: NamedByHash) = map[tx.id]

    override fun put(tx: NamedByHash, zktx: NamedByZKMerkleTree) = map.put(tx.id, zktx.merkleTree.root)
}
