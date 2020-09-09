package com.ing.zknotary.common.zkp

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class PublicInput(
    /**
     * The id of the transaction to verify the proof for
     */
    val transactionId: SecureHash,

    /**
     * Nonce [SecureHash] for the input at index [Int]
     */
    val inputNonces: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] for the input at index [Int]
     */
    val inputHashes: List<SecureHash>,

    /**
     * Nonce [SecureHash] for the input at index [Int]
     */
    val referenceNonces: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] for the reference at index [Int]
     */
    val referenceHashes: List<SecureHash>
)
