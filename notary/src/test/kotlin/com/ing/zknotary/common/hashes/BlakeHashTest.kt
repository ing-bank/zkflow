package com.ing.zknotary.common.hashes

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.crypto.BLAKE2s256DigestService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.time.Duration

class BlakeHashTest {
    private val circuitFolder: String = javaClass.getResource("/TestBlakeHash").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private val blake2sDigestService = BLAKE2s256DigestService

    init {
        zincZKService.setup()
    }

    @AfterEach
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc verifies Blake2s`() {
        val value = 2

        val witness = ByteBuffer.allocate(4).putInt(value).array()
        val expected = blake2sDigestService.hash(witness).bytes

        val preimage = witness.map { "\"${it.asUnsigned()}\"" }
        val publicData = expected.map { "\"${it.asUnsigned()}\"" }

        val proof = zincZKService.prove("{\"preimage\": $preimage}".toByteArray())
        zincZKService.verify(proof, "$publicData".toByteArray())
    }
}
