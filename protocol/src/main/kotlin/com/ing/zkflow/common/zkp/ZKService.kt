package com.ing.zkflow.common.zkp

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

interface ZKService {
    fun prove(witness: Witness): ByteArray
    fun verify(proof: ByteArray, publicInput: PublicInput)

    /**
     * this is used to get the logger for the caller of the caller.
     */
    private val loggerForMyCaller: Logger
        get() = LoggerFactory.getLogger(Throwable().stackTrace[2].className)

    fun proveTimed(witness: Witness, log: Logger = loggerForMyCaller): ByteArray {
        val timedValue = measureTimedValue {
            this.prove(witness)
        }
        log.debug("[prove] ${timedValue.duration}")
        return timedValue.value
    }

    fun verifyTimed(proof: ByteArray, publicInput: PublicInput, log: Logger = loggerForMyCaller) {
        val time = measureTime {
            this.verify(proof, publicInput)
        }
        log.debug("[verify] $time")
    }

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
