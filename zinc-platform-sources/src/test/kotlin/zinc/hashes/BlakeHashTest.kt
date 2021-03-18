package zinc.hashes

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.crypto.blake2s256
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.crypto.DigestService
import org.junit.jupiter.api.AfterAll
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
        val expected = DigestService.blake2s256.hash(witness).bytes

        val preimage = witness.map { "\"${it.asUnsigned()}\"" }
        val publicData = expected.map { "\"${it.asUnsigned()}\"" }

        val proof = zincZKService.prove("{\"preimage\": $preimage}".toByteArray())
        zincZKService.verify(proof, "$publicData".toByteArray())
    }
}
