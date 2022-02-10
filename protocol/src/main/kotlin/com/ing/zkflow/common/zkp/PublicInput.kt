package com.ing.zkflow.common.zkp

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class PublicInput(
    /**
     * Leaf hash [SecureHash] of input components
     */
    val inputComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of output components
     */
    val outputComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of reference components
     */
    val referenceComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of attachment components
     */
    val attachmentComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of command components
     */
    val commandComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of notary components
     */
    val notaryComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of parameters components
     */
    val parametersComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of timewindow components
     */
    val timeWindowComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of signer components
     */
    val signersComponentHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of the UTXO pointed to by the input at index [Int]
     */
    val inputUtxoHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of the UTXO pointed to by the reference at index [Int]
     */
    val referenceUtxoHashes: List<SecureHash>
)
