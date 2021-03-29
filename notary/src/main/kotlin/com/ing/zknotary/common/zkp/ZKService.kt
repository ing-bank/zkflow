package com.ing.zknotary.common.zkp

interface ZKService {
    fun prove(witness: Witness): ByteArray
    fun verify(proof: ByteArray, publicInput: PublicInput)
}

class ZKVerificationException(message: String? = null) : Throwable(message)
class ZKProvingException(message: String? = null) : Throwable(message)
class ZKRunException(message: String? = null) : Throwable(message)
