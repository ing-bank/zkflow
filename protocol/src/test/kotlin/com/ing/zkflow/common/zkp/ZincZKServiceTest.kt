package com.ing.zkflow.common.zkp

import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import kotlin.test.assertFailsWith

class ZincZKServiceTest {
    private val log = loggerFor<ZincZKServiceTest>()
    private val circuitFolder = javaClass.getResource("/ZincZKService").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(30),
        provingTimeout = Duration.ofSeconds(30),
        verificationTimeout = Duration.ofSeconds(1)
    )

    init {
        zincZKService.setup()
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `factory creates a valid setup`() {
        assert(File(zincZKService.compiledCircuitPath).exists())
        assert(File(zincZKService.zkSetup.provingKeyPath).exists())
        assert(File(zincZKService.zkSetup.verifyingKeyPath).exists())
    }

    @Test
    fun `service can prove and verify`() {
        val proof = zincZKService.prove("{\"secret\": \"2\"}")
        val correctPublicData = "\"4\""

        zincZKService.verify(proof, correctPublicData)
    }

    @Test
    fun `proving fails on malformed secret data`() {
        assertFailsWith(ZKProvingException::class) {
            zincZKService.prove("{\"secre\": \"2\"}")
        }
    }

    @Test
    fun `verification fails on public data mismatch`() {
        val proof = zincZKService.prove("{\"secret\": \"2\"}")
        val wrongPublicData = "\"5\""

        assertFailsWith(ZKVerificationException::class) {
            zincZKService.verify(proof, wrongPublicData)
        }
    }

    @Test
    fun `service can prove and verify when reusing setup`() {
        val newZincZKService = ZincZKService(
            circuitFolder,
            artifactFolder = circuitFolder,
            buildTimeout = Duration.ofSeconds(5),
            setupTimeout = Duration.ofSeconds(30),
            provingTimeout = Duration.ofSeconds(30),
            verificationTimeout = Duration.ofSeconds(1)
        )

        // not executing setup, expecting setup artifacts to be in right place already

        val proof = newZincZKService.prove("{\"secret\": \"2\"}")
        val correctPublicData = "\"4\""

        newZincZKService.verify(proof, correctPublicData)
    }
}
