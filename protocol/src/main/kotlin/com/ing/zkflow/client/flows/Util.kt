package com.ing.zkflow.client.flows

import com.ing.zkflow.common.contracts.ZKUpgradeCommandData
import com.ing.zkflow.common.transactions.NotarisedTransactionPayload
import com.ing.zkflow.common.transactions.PrivateNotarisedTransactionPayload
import com.ing.zkflow.common.transactions.PublicNotarisedTransactionPayload
import com.ing.zkflow.common.versioning.generateUpgradeCommandClassName
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.internal.objectOrNewInstance
import net.corda.core.internal.packageName
import net.corda.core.node.ServiceHub
import net.corda.core.node.StatesToRecord
import java.security.PublicKey
import kotlin.reflect.KClass

fun ServiceHub.createSignature(zktxId: SecureHash, publicKey: PublicKey): TransactionSignature {
    val signatureMetadata = SignatureMetadata(
        myInfo.platformVersion,
        Crypto.findSignatureScheme(publicKey).schemeNumberID
    )
    val signableData = SignableData(zktxId, signatureMetadata)
    return keyManagementService.sign(signableData, publicKey)
}

fun ServiceHub.recordTransactions(notarised: NotarisedTransactionPayload, statesToRecord: StatesToRecord = StatesToRecord.ONLY_RELEVANT) {
    getCordaServiceFromConfig<ZKWritableVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE).addTransaction(notarised.svtx)
    when (notarised) {
        is PrivateNotarisedTransactionPayload -> {
            recordTransactions(statesToRecord, listOf(notarised.stx))
        }
        is PublicNotarisedTransactionPayload -> {
            // If we only have the public data, we build a fake SignedTransaction for storage.
            // This way, the publicly visible outputs of this transaction are stored in the vault.
            // Unfortunately, this is impossible for now because the Vault storage logic switches on an expected `WireTransaction`.
            // recordTransactions(statesToRecord, listOf(SignedTransaction(notarised.svtx.tx, notarised.svtx.sigs)))
        }
    }
}

fun getUpgradeCommand(
    currentInput: KClass<out ContractState>,
    nextVersionKClass: KClass<out ContractState>,
    isPrivate: Boolean
): ZKUpgradeCommandData {
    val upgradeCommandName = generateUpgradeCommandClassName(currentInput, nextVersionKClass, isPrivate)
    val upgradeCommandPackage = currentInput.packageName
    val upgradeCommandFqn = "$upgradeCommandPackage.$upgradeCommandName"
    return (
        Class.forName(upgradeCommandFqn).kotlin.objectOrNewInstance() as? ZKUpgradeCommandData
            ?: error("Upgrade command '$upgradeCommandFqn' is not a ZKUpgradeCommandData")
        )
}
