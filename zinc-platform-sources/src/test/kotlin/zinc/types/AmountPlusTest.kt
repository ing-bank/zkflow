package zinc.types

import com.ing.zknotary.common.zkp.ZKProvingException
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.contracts.Amount
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AmountPlusTest {
    private val log = loggerFor<AmountPlusTest>()

    private val circuitFolder: String = AmountPlusTest::class.java.getResource("/AmountPlusTest").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private val dummyToken = "com.ing.zknotary.SuperToken"
    private val anotherDummyToken = 420

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc plus fails due to different token sizes`() {
        val left = Amount(200, BigDecimal("10"), dummyToken)
        val right = Amount(100, BigDecimal("1"), dummyToken)

        val input = toWitness(left, right)

        val exception = Assertions.assertThrows(ZKProvingException::class.java) {
            zincZKService.proveTimed(input, log)
        }

        Assertions.assertTrue(
            exception.message?.contains("Token sizes don't match") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc plus fails due to different token hashes`() {
        val left = Amount(1, BigDecimal("1"), dummyToken)
        val right = Amount(1, BigDecimal("1"), anotherDummyToken)

        val input = toWitness(left, right)

        val exception = Assertions.assertThrows(ZKProvingException::class.java) {
            zincZKService.proveTimed(input, log)
        }

        Assertions.assertTrue(
            exception.message?.contains("Tokens don't match") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc plus smoke test`() {
        val left = Amount(100, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("1"), dummyToken)

        val input = toWitness(left, right)
        val expected = left.plus(right).toJSON()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }
}
