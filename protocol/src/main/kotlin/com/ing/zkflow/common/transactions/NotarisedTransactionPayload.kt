package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.transactions.verification.ZKTransactionVerifierService
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import net.corda.core.node.ServiceHub
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

    fun verify(
        serviceHub: ServiceHub,
        checkSufficientSignatures: Boolean
    ) {
        // We only need to verify the svtx: because id of stx is identical, it means the stx is also valid.
        val zkTransactionVerifierService = ZKTransactionVerifierService(
            serviceHub,
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)
        )

        zkTransactionVerifierService.verify(svtx, checkSufficientSignatures)
    }
}
