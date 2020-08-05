package com.ing.zknotary.common.zkp

import org.junit.After
import org.junit.Test
import java.io.File
import java.time.Duration
import kotlin.test.assertFailsWith

class ZincZKServiceTest {
    private val circuitSourcePath: String = javaClass.getResource("/ZincZKService/main.zn").path
    private val zincZKService = ZincZKService(
        circuitSrcPath = circuitSourcePath,
        artifactFolder = File(circuitSourcePath).parent,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(30),
        provingTimeout = Duration.ofSeconds(30),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private var zincFiles = listOf(
        zincZKService.compiledCircuitPath,
        zincZKService.zkSetup.provingKeyPath,
        zincZKService.zkSetup.verifyingKeyPath
    )

    init {
        zincZKService.setup()
    }

    @After
    fun `remove zinc files`() {
        zincFiles.map { File(it).delete() }
    }

    @Test
    fun `factory creates a valid setup`() {
        assert(File(zincZKService.compiledCircuitPath).exists())
        assert(File(zincZKService.zkSetup.provingKeyPath).exists())
        assert(File(zincZKService.zkSetup.verifyingKeyPath).exists())
    }

    @Test
    fun `service can prove and verify`() {
        val proof = zincZKService.prove("{\"secret\": \"2\"}".toByteArray())
        val correctPublicData = "\"4\"".toByteArray()

        zincZKService.verify(proof, correctPublicData)
    }

    @Test
    fun `proving fails on malformed secret data`() {
        assertFailsWith(ZKProvingException::class) {
            zincZKService.prove("{\"secre\": \"2\"}".toByteArray())
        }
    }

    @Test
    fun `verification fails on public data mismatch`() {
        val proof = zincZKService.prove("{\"secret\": \"2\"}".toByteArray())
        val wrongPublicData = "\"5\"".toByteArray()

        assertFailsWith(ZKVerificationException::class) {
            zincZKService.verify(proof, wrongPublicData)
        }
    }

    @Test
    fun `service can prove and verify when reusing setup`() {
        val newZincZKService = ZincZKService(
            circuitSrcPath = circuitSourcePath,
            artifactFolder = File(circuitSourcePath).parent,
            buildTimeout = Duration.ofSeconds(5),
            setupTimeout = Duration.ofSeconds(30),
            provingTimeout = Duration.ofSeconds(30),
            verificationTimeout = Duration.ofSeconds(1)
        )

        // not executing setup, expecting setup artifacts to be in right place already

        val proof = newZincZKService.prove("{\"secret\": \"2\"}".toByteArray())
        val correctPublicData = "\"4\"".toByteArray()

        newZincZKService.verify(proof, correctPublicData)
    }
}
