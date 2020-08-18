package com.ing.zknotary.common.hashes

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.util.BLAKE2s256ReversedDigestService
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.PedersenDigestService
import org.junit.After
import org.junit.Test
import java.nio.ByteBuffer
import java.time.Duration

class MerkleRootTest {
    private val circuitFolder: String = javaClass.getResource("/TestMerkleRoot").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private val blake2sDigestService = BLAKE2s256DigestService
    private val blake2sReversedDigestService = BLAKE2s256ReversedDigestService
    private val pedersenDigestService = PedersenDigestService

    init {
        zincZKService.setup()
    }

    @After
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc verifies Merkle root - Blake2s and Pedersen`() {
        fun Byte.reverseBits(): Byte {
            var x = this.toInt()
            var y: Byte = 0
            for (position in 8 - 1 downTo 0) {
                y = (y + (x and 1 shl position).toByte()).toByte()
                x = (x shr 1)
            }
            return y
        }

        val values = listOf(1, 2, 3, 4)

        val witness = values.map {
            ByteBuffer.allocate(4).putInt(it).array()
        }

        val level0 = witness.map { ba ->
            blake2sDigestService.hash(ba).bytes.map { it.reverseBits() }.toByteArray()
        }

        val level1 = listOf(
            pedersenDigestService.hash(level0[0] + level0[1]).bytes,
            pedersenDigestService.hash(level0[2] + level0[3]).bytes
        )

        val root = pedersenDigestService.hash(level1[0] + level1[1]).bytes

        val leaves = witness.map { input -> input.map { "\"${it.reverseBits().asUnsigned()}\"" } }
        val publicData = root.map { "\"${it.asUnsigned()}\"" }

        val proof = zincZKService.prove("{\"leaves\": $leaves}".toByteArray())
        zincZKService.verify(proof, "$publicData".toByteArray())
    }

    @Test
    fun `zinc verifies Merkle root - Blake2sRev and Pedersen`() {
        val values = listOf(1, 2, 3, 4)

        val witness = values.map {
            ByteBuffer.allocate(4).putInt(it).array()
        }

        val level0 = witness.map { blake2sReversedDigestService.hash(it).bytes }

        val level1 = listOf(
            pedersenDigestService.hash(level0[0] + level0[1]).bytes,
            pedersenDigestService.hash(level0[2] + level0[3]).bytes
        )

        val root = pedersenDigestService.hash(level1[0] + level1[1]).bytes

        val leaves = witness.map { input -> input.map { "\"${it.asUnsigned()}\"" } }
        val publicData = root.map { "\"${it.asUnsigned()}\"" }

        val proof = zincZKService.prove("{\"leaves\": $leaves}".toByteArray())
        zincZKService.verify(proof, "$publicData".toByteArray())
    }
}
