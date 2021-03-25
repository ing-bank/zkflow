package com.ing.zknotary.client.flows

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import java.security.PublicKey

fun ServiceHub.createSignature(zktxId: SecureHash, publicKey: PublicKey): TransactionSignature {
    val signatureMetadata = SignatureMetadata(
        myInfo.platformVersion,
        Crypto.findSignatureScheme(publicKey).schemeNumberID
    )
    val signableData = SignableData(zktxId, signatureMetadata)
    return keyManagementService.sign(signableData, publicKey)
}

fun ServiceHub.recordTransactions(stx: SignedTransaction, svtx: SignedZKVerifierTransaction) {

    val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage =
        getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)
    zkVerifierTransactionStorage.addTransaction(svtx)
    recordTransactions(stx)
}
