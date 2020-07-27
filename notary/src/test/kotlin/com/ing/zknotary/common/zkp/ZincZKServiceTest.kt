package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.util.Result
import org.junit.After
import org.junit.Test
import java.io.File
import java.time.Duration

class ZincZKServiceTest {
    // Main circuit file MUST be called main.zn
    private val circuitSourcePath: String = javaClass.getResource("/ZincZKService/main.zn").path
    private val timeout = Duration.ofSeconds(5)

    private var zincFiles = listOf<String>()

    @After
    fun `remove zinc files`() {
        zincFiles.map { File(it).delete() }
    }

    @Test
    fun `factory creates a valid setup`() {
        val folder = File(circuitSourcePath).parent
        val zincZKService = ZincZKServiceFactory.create(
            circuitSourcePath, folder,
            buildTimeout = timeout, setupTimeout = timeout,
            provingTimeout = timeout, verifyingTimeout = timeout
        )

        zincFiles = listOf(
            zincZKService.compiledCircuit, zincZKService.zkSetup.provingKeyPath!!, zincZKService.zkSetup.verifyingKeyPath!!
        )

        assert(File(zincZKService.compiledCircuit).exists())
        assert(File(zincZKService.zkSetup.provingKeyPath).exists())
        assert(File(zincZKService.zkSetup.verifyingKeyPath).exists())
    }

    @Test
    fun `service can prove and verify`() {
        val folder = File(circuitSourcePath).parent
        val zincZKService = ZincZKServiceFactory.create(
            circuitSourcePath, folder, buildTimeout = timeout, setupTimeout = timeout,
            provingTimeout = timeout, verifyingTimeout = timeout
        )

        val proofResult = zincZKService.prove("{}".toByteArray())
        assert(proofResult is Result.Success)
        val proof = proofResult.expect("Proof generation must have been successful")

        val verifyResult = zincZKService.verify(proof)
        assert(verifyResult is Result.Success)
    }
}
