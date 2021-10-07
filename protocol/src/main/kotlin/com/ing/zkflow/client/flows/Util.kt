package com.ing.zkflow.client.flows

import com.ing.zkflow.common.transactions.NotarisedTransactionPayload
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.node.StatesToRecord
import java.security.PublicKey

fun ServiceHub.createSignature(zktxId: SecureHash, publicKey: PublicKey): TransactionSignature {
    val signatureMetadata = SignatureMetadata(
        myInfo.platformVersion,
        Crypto.findSignatureScheme(publicKey).schemeNumberID
    )
    val signableData = SignableData(zktxId, signatureMetadata)
    return keyManagementService.sign(signableData, publicKey)
}

fun ServiceHub.recordTransactions(notarised: NotarisedTransactionPayload, statesToRecord: StatesToRecord = StatesToRecord.ONLY_RELEVANT) {

    getCordaServiceFromConfig<ZKWritableVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE).addTransaction(
        notarised.svtx
    )
    recordTransactions(statesToRecord, listOf(notarised.stx))
}
