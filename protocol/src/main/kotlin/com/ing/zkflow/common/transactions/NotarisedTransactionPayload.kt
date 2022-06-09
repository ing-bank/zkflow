package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.transactions.verification.ZKTransactionVerifierService
import com.ing.zkflow.common.zkp.ZKTransactionService
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction

sealed interface NotarisedTransactionPayload {
    val svtx: SignedZKVerifierTransaction

    fun verify(serviceHub: ServiceHub, zkService: ZKTransactionService, checkSufficientSignatures: Boolean) =
        ZKTransactionVerifierService(serviceHub, zkService).verify(svtx, checkSufficientSignatures)
}

@CordaSerializable
data class PrivateNotarisedTransactionPayload(
    override val svtx: SignedZKVerifierTransaction,
    val stx: SignedTransaction
) : NotarisedTransactionPayload {
    init {
        require(svtx.id == stx.id) { "Illegal FinalizedTransactionPayload: transaction id's do not match. stx: ${stx.id}, svtx: ${svtx.id}" }
        require(svtx.sigs == stx.sigs) { "Illegal FinalizedTransactionPayload: transaction signatures do not match. stx: ${stx.sigs}, svtx: ${svtx.sigs}" }
    }

    fun toPublic(): PublicNotarisedTransactionPayload = PublicNotarisedTransactionPayload(svtx)
}

@CordaSerializable
data class PublicNotarisedTransactionPayload(
    override val svtx: SignedZKVerifierTransaction
) : NotarisedTransactionPayload
