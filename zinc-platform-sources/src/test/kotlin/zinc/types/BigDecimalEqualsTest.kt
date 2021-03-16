package zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BigDecimalEqualsTest {
    private val log = loggerFor<BigDecimalEqualsTest>()

    private val circuitFolder: String = BigDecimalEqualsTest::class.java.getResource("/BigDecimalEqualsTest").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc compares using equals (sign is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("-421.42")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares using equals (integer is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("42.42")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares using equals (fraction is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.421")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares using equals (both are equal to each other)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.42")

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }
}
