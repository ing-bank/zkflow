package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.ZKProverTransaction
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable

// TODO: remove this annotation when it is no longer necessary: when ZincSerialization can also deserialize (for testing)
@CordaSerializable
data class Witness(
    val transaction: ZKProverTransaction,
    val inputNonces: List<SecureHash>,
    val referenceNonces: List<SecureHash>
)
