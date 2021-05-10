package zinc.types

import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BigDecimalComparisonTest {
    private val log = loggerFor<BigDecimalComparisonTest>()
    private val zincZKService = getZincZKService<BigDecimalComparisonTest>()

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

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (right sign is greater)`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal("1.1")

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (left integer is greater)`() {
        val left = BigDecimal.ONE
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (right integer is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal.ONE

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (left fraction is greater)`() {
        val left = BigDecimal("0.1")
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two positives (right fraction is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("0.1")

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two number of the same digits, but in opposite order (smoke test for big-endianness)`() {
        val left = BigDecimal("123")
        val right = BigDecimal("321")

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc compares two zeros`() {
        val zero = BigDecimal.ZERO

        val input = toWitness(zero, zero)
        val expected = "\"${zero.compareTo(zero)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc comparison of ZERO and ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = "\"${left.compareTo(right)}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }
}
