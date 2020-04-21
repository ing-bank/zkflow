package com.ing.zknotary.common.zkp

import net.corda.core.CordaException
import net.corda.core.KeepForDJVM
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializeAsToken

interface ZKVerifier: SerializeAsToken {
    @Throws(ZKProofVerificationException::class)
    fun verify(proof: Proof, instance: ByteArray)
}

@KeepForDJVM
@CordaSerializable
class ZKProofVerificationException(reason: String) :
    CordaException("Transaction cannot be verified. Reason: $reason")

