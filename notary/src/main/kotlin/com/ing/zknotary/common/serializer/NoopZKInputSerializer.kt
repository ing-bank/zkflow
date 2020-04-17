package com.ing.zknotary.common.serializer

import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.LedgerTransaction

object NoopZKInputSerializer : ZKInputSerializer {
    override fun serializeWitness(tx: LedgerTransaction, signatures: List<ByteArray>) = ByteArray(0)
    override fun serializeInstance(zkTransactionId: SecureHash) = ByteArray(0)
}