package com.ing.zkflow.common.zkp

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class PublicInput(
    /**
     * Leaf hash [SecureHash] of outputs
     */
    val outputComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of the UTXO pointed to by the input StateRefs at index [Int]
     */
    val inputUtxoHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of the UTXO pointed to by the reference StateRefs at index [Int]
     */
    val referenceUtxoHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of attachment components
     */
    val attachmentComponentHashes: List<SecureHash>,

    /**
     * Serialized form of the commandComponents, so no the leaf hashes
     */
    val commandComponents: List<ByteArray>,

    /**
     * Serialized form of the notaryComponents, so no the leaf hashes
     */
    val notaryComponents: List<ByteArray>,

    /**
     * Serialized form of the parametersComponents, so no the leaf hashes
     */
    val parametersComponents: List<ByteArray>,

    /**
     * Serialized form of the timeWindowComponents, so no the leaf hashes
     */
    val timeWindowComponents: List<ByteArray>,

    /**
     * Serialized form of the signersComponents, so no the leaf hashes
     */
    val signersComponents: List<ByteArray>,

    /**
     * Serialized form of the inputStateRefs, so no the leaf hashes
     */
    val inputStateRefs: List<ByteArray>,
)
