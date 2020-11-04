package com.ing.zknotary.common.hashes

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.crypto.PedersenDigestService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.time.Duration

class PedersenHashTest {
    private val circuitFolder: String = javaClass.getResource("/TestPedersenHash").path
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
    fun `zinc verifies Pedersen`() {
        val value = 2
        val witness = ByteBuffer.allocate(4).putInt(value).array()
        val expected = PedersenDigestService.hash(witness).bytes

        val preimage = witness.map { "\"${it.asUnsigned()}\"" }
        val publicData = expected.map { "\"${it.asUnsigned()}\"" }

        val proof = zincZKService.prove("{\"preimage\": $preimage}".toByteArray())
        zincZKService.verify(proof, "$publicData".toByteArray())
    }
}
