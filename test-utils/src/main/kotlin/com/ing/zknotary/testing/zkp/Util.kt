package com.ing.zknotary.testing.zkp

import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZincZKService
import org.slf4j.Logger
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

public inline fun <reified T : Any> getZincZKService(
    buildTimeout: Duration = Duration.ofSeconds(5),
    setupTimeout: Duration = Duration.ofSeconds(300),
    provingTimeout: Duration = Duration.ofSeconds(300),
    verificationTimeout: Duration = Duration.ofSeconds(1)
): ZincZKService {
    val circuitFolder: String = T::class.java.getResource("/${T::class.java.simpleName}")!!.path
    return ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = buildTimeout,
        setupTimeout = setupTimeout,
        provingTimeout = provingTimeout,
        verificationTimeout = verificationTimeout,
    )
}

@ExperimentalTime
public fun ZincZKService.setupTimed(log: Logger) {
    val time = measureTime {
        this.setup()
    }
    log.debug("[setup] $time")
}

@ExperimentalTime
public fun ZincZKService.proveTimed(witness: Witness, log: Logger): ByteArray {
    var proof: ByteArray
    val time = measureTime {
        proof = this.prove(witness)
    }
    log.debug("[prove] $time")
    return proof
}

@ExperimentalTime
public fun ZincZKService.proveTimed(witnessJson: String, log: Logger): ByteArray {
    var proof: ByteArray
    val time = measureTime {
        proof = this.prove(witnessJson)
    }
    log.debug("[prove] $time")
    return proof
}

@ExperimentalTime
public fun ZincZKService.verifyTimed(proof: ByteArray, publicInputJson: String, log: Logger) {
    val time = measureTime {
        this.verify(proof, publicInputJson)
    }
    log.debug("[verify] $time")
}

@ExperimentalTime
public fun ZincZKService.verifyTimed(proof: ByteArray, publicInput: PublicInput, log: Logger) {
    val time = measureTime {
        this.verify(proof, publicInput)
    }
    log.debug("[verify] $time")
}
