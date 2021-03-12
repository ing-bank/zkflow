package zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.time.ExperimentalTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalTime
class BigDecimalComparisonTest {
    private val log = loggerFor<BigDecimalComparisonTest>()

    private val circuitFolder: String = BigDecimalComparisonTest::class.java.getResource("/BigDecimalComparisonTest").path
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
    fun `zinc compares two positives (left sign is greater)`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal("-1.1")

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (right sign is greater)`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal("1.1")

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (left integer is greater)`() {
        val left = BigDecimal.ONE
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (right integer is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal.ONE

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (left fraction is greater)`() {
        val left = BigDecimal("0.1")
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two positives (right fraction is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("0.1")

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two number of the same digits, but in opposite order (smoke test for big-endianness)`() {
        val left = BigDecimal("123")
        val right = BigDecimal("321")

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `zinc compares two zeros`() {
        val zero = BigDecimal.ZERO

        val input = "{\"left\": ${zero.toJSON()}, \"right\": ${zero.toJSON()}}"
        val expected = "\"${zero.compareTo(zero)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input.toByteArray(), log).let {
            zincZKService.verifyTimed(it, expected.toByteArray(), log)
        }
    }
}
