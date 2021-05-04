package com.ing.zknotary.zinc.hashes

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.crypto.blake2s256
import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.corda.core.crypto.DigestService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.time.Duration

class BlakeHashMultiTest {
    private val circuitFolder: String = javaClass.getResource("/TestBlakeHashMulti").path
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
    fun `zinc verifies Blake2s`() {
        val value = 2

        val witness = ByteBuffer.allocate(4).putInt(value).array()
        val expected = DigestService.blake2s256.hash(witness + witness).bytes

        val witnessJson = "{\"preimage\": [${witness.map { "\"${it.asUnsigned()}\"" }}, ${witness.map { "\"${it.asUnsigned()}\"" }}]}"
        val publicDataJson = Json.encodeToString(expected.map { it.asUnsigned().toString() })

        zincZKService.run(witnessJson, publicDataJson)

        val proof = zincZKService.prove(witnessJson)
        zincZKService.verify(proof, publicDataJson)
    }
}
