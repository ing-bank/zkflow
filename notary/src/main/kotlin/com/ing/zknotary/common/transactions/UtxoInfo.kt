package com.ing.zknotary.common.transactions

import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKTransactionResolutionException
import com.ing.zknotary.node.services.ZKVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.algorithm
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.CordaSerializable

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

        val resolvedOutput = zkTxStorage.getTransaction(stateRef.txhash)?.tx?.outputHashes?.get(stateRef.index)
            ?: ZKTransactionResolutionException(stateRef.txhash)

        val calculatedUtxoHash = SecureHash.componentHashAs(
            stateRef.txhash.algorithm, // This works, as long as there is *one* DigestAlgorithm for a transaction, which provides different hash functions for leaves and nodes. Here we need the one for leaves: `componentHashAs()`
            nonce.copyBytes() + serializedContents
        )

        require(resolvedOutput == calculatedUtxoHash) { "Calculated UTXO hash '$calculatedUtxoHash' does not match resolved hash '$resolvedOutput'" }
    }
}
