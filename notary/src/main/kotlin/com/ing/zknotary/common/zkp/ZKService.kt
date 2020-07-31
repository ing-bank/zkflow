package com.ing.zknotary.common.zkp

import net.corda.core.serialization.SerializeAsToken

interface ZKService : SerializeAsToken {
    fun prove(witness: ByteArray): ByteArray
    fun verify(proof: ByteArray, publicInput: ByteArray)
}

class ZKVerificationException(message: String? = null) : Throwable(message)
class ZKProvingException(message: String? = null) : Throwable(message)
