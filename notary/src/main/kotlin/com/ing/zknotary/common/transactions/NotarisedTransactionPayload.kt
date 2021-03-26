package com.ing.zknotary.common.transactions

import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction

@CordaSerializable
data class NotarisedTransactionPayload(
    val svtx: SignedZKVerifierTransaction,
    val stx: SignedTransaction
) {
    init {
        require(svtx.id == stx.id) { "Illegal FinalizedTransactionPayload: transaction id's do not match. stx: ${stx.id}, svtx: ${svtx.id}" }
        require(svtx.sigs == stx.sigs) { "Illegal FinalizedTransactionPayload: transaction signatures do not match. stx: ${stx.sigs}, svtx: ${svtx.sigs}" }
    }
}
