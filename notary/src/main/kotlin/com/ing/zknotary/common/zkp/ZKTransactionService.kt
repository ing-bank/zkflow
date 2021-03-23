package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.algorithm
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.WireTransaction

@CordaSerializable
class UtxoInfo(
    val stateRef: StateRef,
    val serializedContents: ByteArray,
    val nonce: SecureHash
) {
    /**
     * This function verifies that the serialized content hashed with the nonce matches
     * the actual output hash for StateRef in the ZKP chain that was previously resolved
     */
    fun verify(serviceHub: ServiceHub) {
        val zkTxStorage: ZKVerifierTransactionStorage =
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)

        val calculatedUtxoHash = SecureHash.componentHashAs(
            stateRef.txhash.algorithm, // This works, as long as there is *one* DigestAlgorithm for a transaction, which provides the hash functions for leaves and nodes. Here we need the one for the node: `componentHashas()`
            nonce.copyBytes() + serializedContents
        )

        val resolvedOutput = zkTxStorage.getTransaction(stateRef.txhash)?.tx?.outputHashes?.get(0)
            ?: error("Coulnd't resolve zkvtx with id ${stateRef.txhash}")

        require(resolvedOutput == calculatedUtxoHash) { "Calculated UTXO hash '$calculatedUtxoHash' does not match resolved hash '$resolvedOutput'" }
    }
}

interface ZKTransactionService : SerializeAsToken {
    fun prove(
        wtx: WireTransaction
    ): ZKVerifierTransaction

    fun verify(stx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean)
}
