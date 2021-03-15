package com.ing.zknotary.client.flows

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowLogic
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import java.security.PublicKey

fun FlowLogic<Any>.signInitialZKTransaction(
    vtx: ZKVerifierTransaction,
    publicKey: PublicKey = serviceHub.myInfo.legalIdentitiesAndCerts.single().owningKey,
    signatureMetadata: SignatureMetadata = SignatureMetadata(serviceHub.myInfo.platformVersion, Crypto.findSignatureScheme(publicKey).schemeNumberID)
): SignedZKVerifierTransaction {
    val signableData = SignableData(vtx.id, signatureMetadata)
    val sig = this.serviceHub.keyManagementService.sign(signableData, publicKey)
    return SignedZKVerifierTransaction(vtx, listOf(sig))
}

fun ServiceHub.createSignature(zktxId: SecureHash, publicKey: PublicKey): TransactionSignature {
    val signatureMetadata = SignatureMetadata(
        myInfo.platformVersion,
        Crypto.findSignatureScheme(publicKey).schemeNumberID
    )
    val signableData = SignableData(zktxId, signatureMetadata)
    return keyManagementService.sign(signableData, publicKey)
}

fun ServiceHub.recordTransactions(stx: SignedTransaction, svtx: SignedZKVerifierTransaction) {

    val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage = getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)
    zkVerifierTransactionStorage.addTransaction(svtx)
    recordTransactions(stx)
}
