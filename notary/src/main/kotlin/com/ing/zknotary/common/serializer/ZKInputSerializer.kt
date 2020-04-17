package com.ing.zknotary.common.serializer

import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.LedgerTransaction

interface ZKInputSerializer {
    fun serializeWitness(tx: LedgerTransaction, signatures: List<ByteArray>): ByteArray

    fun serializeInstance(zkTransactionId: SecureHash): ByteArray
}

