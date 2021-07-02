package com.ing.zknotary.common.zkp

interface ZKService {
    fun prove(witness: Witness): ByteArray
    fun verify(proof: ByteArray, publicInput: PublicInput)

    /**
     * Run the circuit with the given witness and input.
     *
     * @return the output of the run, when available.
     */
    fun run(witness: Witness, publicInput: PublicInput): String? {
        val byteArray = prove(witness)
        verify(byteArray, publicInput)
        return null
    }
}

class ZKVerificationException(message: String? = null) : Throwable(message)
class ZKProvingException(message: String? = null) : Throwable(message)
class ZKRunException(message: String? = null) : Throwable(message)
