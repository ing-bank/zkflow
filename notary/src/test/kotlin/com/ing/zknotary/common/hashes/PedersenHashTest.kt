package com.ing.zknotary.common.hashes

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.crypto.PedersenDigestService
import org.junit.After
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.time.Duration

class PedersenHashTest {
    private val circuitSourcePath: String = javaClass.getResource("/TestPedersenHash/src/main.zn").path
    private val zincZKService = ZincZKService(
        circuitSrcPath = circuitSourcePath,
        artifactFolder = File(circuitSourcePath).parent,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(30),
        provingTimeout = Duration.ofSeconds(30),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private val pedersenDigestService = PedersenDigestService

    init {
        zincZKService.setup()
    }

    @After
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc verifies Pedersen`() {
        val value = 2
        val witness = ByteBuffer.allocate(4).putInt(value).array()
        val expected = pedersenDigestService.hash(witness).bytes

        val preimage = witness.map { "\"${it.asUnsigned()}\"" }
        val publicData = expected.map { "\"${it.asUnsigned()}\"" }

        val proof = zincZKService.prove("{\"preimage\": $preimage}".toByteArray())
        zincZKService.verify(proof, "$publicData".toByteArray())
    }
}
