package com.ing.zknotary.client.flows

import com.ing.zknotary.common.transactions.NamedByZKMerkleTree
import com.ing.zknotary.common.transactions.ZKProverTransaction
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowLogic
import net.corda.core.node.ServiceHub
import java.security.PublicKey

fun FlowLogic<Any>.signInitialZKTransaction(
    ztx: NamedByZKMerkleTree,
    publicKey: PublicKey = serviceHub.myInfo.legalIdentitiesAndCerts.single().owningKey,
    signatureMetadata: SignatureMetadata = SignatureMetadata(serviceHub.myInfo.platformVersion, Crypto.findSignatureScheme(publicKey).schemeNumberID)
): List<TransactionSignature> {
    val signableData = SignableData(ztx.id, signatureMetadata)
    val sig = this.serviceHub.keyManagementService.sign(signableData, publicKey)
    return listOf(sig)
}

fun ServiceHub.createSignature(zktx: ZKProverTransaction, publicKey: PublicKey): TransactionSignature {
    val signatureMetadata = SignatureMetadata(
        myInfo.platformVersion,
        Crypto.findSignatureScheme(publicKey).schemeNumberID
    )
    val signableData = SignableData(zktx.id, signatureMetadata)
    return keyManagementService.sign(signableData, publicKey)
}
