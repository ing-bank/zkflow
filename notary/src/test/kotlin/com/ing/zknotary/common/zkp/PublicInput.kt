package com.ing.zknotary.common.zkp

import net.corda.core.crypto.SecureHash

data class PublicInput(
    /**
     * The id of the transaction to verify the proof for
     */
    val transactionId: SecureHash,

    /**
     * Nonce [SecureHash] for the input at index [Int]
     */
    val inputNonces: Map<Int, SecureHash>,

    /**
     * Leaf hash [SecureHash] for the input at index [Int]
     */
    val inputHashes: Map<Int, SecureHash>,

    /**
     * Nonce [SecureHash] for the input at index [Int]
     */
    val referenceNonces: Map<Int, SecureHash>,

    /**
     * Leaf hash [SecureHash] for the reference at index [Int]
     */
    val referenceHashes: Map<Int, SecureHash>
)
