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
     * Leaf hash [SecureHash] of the UTXO pointed to by the input at index [Int]
     */
    val inputHashes: List<SecureHash>,

    /**
     * Leaf hash [SecureHash] of the UTXO pointed to by the reference at index [Int]
     */
    val referenceHashes: List<SecureHash>
)
