package com.ing.zkflow.common.transactions

import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.OpaqueBytes

class ZKLedgerTransaction(
    val svtx: SignedZKVerifierTransaction,
    val serializedInputs: Map<Int, OpaqueBytes>,
    val serializedReferences: Map<Int, OpaqueBytes>
) {

    companion object {

        fun fromLedgerTransaction(ltx: LedgerTransaction): ZKLedgerTransaction {
            TODO()
        }
    }
}
