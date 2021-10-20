package com.ing.zkflow.zinc.hashes

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.crypto.blake2s256
import com.ing.zkflow.crypto.pedersen
import net.corda.core.crypto.DigestService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
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

    init {
        zincZKService.setup()
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc verifies Merkle root - Blake2s and Pedersen`() {
        val values = listOf(1, 2, 3, 4)

        val witness = values.map {
            ByteBuffer.allocate(4).putInt(it).array()
        }

        val level0 = witness.map { ba ->
            DigestService.blake2s256.hash(ba).bytes
        }

        val level1 = listOf(
            DigestService.pedersen.hash(level0[0] + level0[1]).bytes,
            DigestService.pedersen.hash(level0[2] + level0[3]).bytes
        )

        val root = DigestService.pedersen.hash(level1[0] + level1[1]).bytes

        val leaves = witness.map { input -> input.map { "\"${it.asUnsigned()}\"" } }
        val publicData = root.map { "\"${it.asUnsigned()}\"" }

        val proof = zincZKService.prove("{\"leaves\": $leaves}")
        zincZKService.verify(proof, "$publicData")
    }
}
