package com.ing.zkflow.testing.zkp

import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZincZKService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

/**
 * this is used to get the logger for the caller of the caller.
 */
private val loggerForMyCaller: Logger
    get() = LoggerFactory.getLogger(Throwable().stackTrace[2].className)

public fun ZincZKService.setupTimed(log: Logger = loggerForMyCaller) {
    val time = measureTime {
        this.setup()
    }
    log.debug("[setup] $time")
}

public fun ZincZKService.proveTimed(witness: Witness, log: Logger = loggerForMyCaller): ByteArray {
    val timedValue = measureTimedValue {
        this.prove(witness)
    }
    log.debug("[prove] ${timedValue.duration}")
    return timedValue.value
}

public fun ZincZKService.proveTimed(witnessJson: String, log: Logger = loggerForMyCaller): ByteArray {
    val timedValue = measureTimedValue {
        this.prove(witnessJson)
    }
    log.debug("[prove] ${timedValue.duration}")
    return timedValue.value
}

public fun ZincZKService.verifyTimed(proof: ByteArray, publicInputJson: String, log: Logger = loggerForMyCaller) {
    val time = measureTime {
        this.verify(proof, publicInputJson)
    }
    log.debug("[verify] $time")
}

public fun ZincZKService.verifyTimed(proof: ByteArray, publicInput: PublicInput, log: Logger = loggerForMyCaller) {
    val time = measureTime {
        this.verify(proof, publicInput)
    }
    log.debug("[verify] $time")
}