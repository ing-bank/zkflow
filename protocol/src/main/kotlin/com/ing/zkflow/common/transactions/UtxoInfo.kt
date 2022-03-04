package com.ing.zkflow.common.transactions

import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.ZKTransactionResolutionException
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.algorithm
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.CordaSerializable
import kotlin.reflect.KClass

@CordaSerializable
class UtxoInfo private constructor(
    val stateRef: StateRef,

    /**
     * Serialized type TransactionState<T: ContractState>
     */
    val serializedContents: ByteArray,
    val nonce: SecureHash,

    /**
     * The only reason we store this as String and not KClass is because Corda serialization does not support it.
     */
    private val stateClassName: String
) {
    companion object {
        fun build(stateRef: StateRef, serializedContents: ByteArray, nonce: SecureHash, stateClass: KClass<out ContractState>): UtxoInfo {
            return UtxoInfo(stateRef, serializedContents, nonce, stateClass.qualifiedStateClassName)
        }
    }

    @Suppress("UNCHECKED_CAST") // The whole idea is that we try it and fail if not what we expect
    val stateClass: KClass<out ContractState> by lazy {
        Class.forName(stateClassName)
            .kotlin
            as? KClass<out ContractState>
            ?: error("$stateClassName is not a ContractState")
    }

    /**
     * This function verifies that the serialized content hashed with the nonce matches
     * the actual output hash for StateRef in the ZKP chain that was previously resolved
     */
    fun verify(serviceHub: ServiceHub) {
        val zkTxStorage: ZKVerifierTransactionStorage =
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)

        val resolvedOutput = zkTxStorage.getTransaction(stateRef.txhash)?.tx?.outputHashes()?.get(stateRef.index)
            ?: ZKTransactionResolutionException(stateRef.txhash)

        val calculatedUtxoHash = SecureHash.componentHashAs(
            stateRef.txhash.algorithm, // This works, as long as there is *one* DigestAlgorithm for a transaction, which provides different hash functions for leaves and nodes. Here we need the one for leaves: `componentHashAs()`
            nonce.copyBytes() + serializedContents
        )

        require(resolvedOutput == calculatedUtxoHash) { "Calculated UTXO hash '$calculatedUtxoHash' does not match resolved hash '$resolvedOutput'" }
    }
}
