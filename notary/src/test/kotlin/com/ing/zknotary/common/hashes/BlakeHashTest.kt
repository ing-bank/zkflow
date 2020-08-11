package com.ing.zknotary.common.hashes

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.util.BLAKE2s256ReversedDigestService
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.crypto.BLAKE2s256DigestService
import org.junit.After
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.time.Duration

class BlakeHashTest {
    private val circuitSourcePath: String = javaClass.getResource("/TestBlakeHash/src/main.zn").path
    private val zincZKService = ZincZKService(
        circuitSrcPath = circuitSourcePath,
        artifactFolder = File(circuitSourcePath).parent,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private val blake2sDigestService = BLAKE2s256DigestService
    private val blake2sReversedDigestService = BLAKE2s256ReversedDigestService

    init {
        zincZKService.setup()
    }

    @After
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc verifies Blake2s`() {

        fun Byte.reverseBits(): Byte {
            var x = this.toInt()
            var y: Byte = 0
            for (position in 8 - 1 downTo 0) {
                y = (y + (x and 1 shl position).toByte()).toByte()
                x = (x shr 1)
            }
            return y
        }

        val value = 2

        val witness = ByteBuffer.allocate(4).putInt(value).array()
        val expected = blake2sDigestService.hash(witness).bytes

        val preimage = witness.map { "\"${it.reverseBits().asUnsigned()}\"" }
        val publicData = expected.map { "\"${it.reverseBits().asUnsigned()}\"" }

        val proof = zincZKService.prove("{\"preimage\": $preimage}".toByteArray())
        zincZKService.verify(proof, "$publicData".toByteArray())
    }

    @Test
    fun `zinc verifies Blake2s reversed`() {
        val value = 2
        val witness = ByteBuffer.allocate(4).putInt(value).array()
        val expected = blake2sReversedDigestService.hash(witness).bytes

        val preimage = witness.map { "\"${it.asUnsigned()}\"" }
        val publicData = expected.map { "\"${it.asUnsigned()}\"" }

        val proof = zincZKService.prove("{\"preimage\": $preimage}".toByteArray())
        zincZKService.verify(proof, "$publicData".toByteArray())
    }
}
