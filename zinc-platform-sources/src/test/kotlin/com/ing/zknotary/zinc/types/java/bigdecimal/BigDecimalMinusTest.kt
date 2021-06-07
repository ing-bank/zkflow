package com.ing.zknotary.zinc.types.java.bigdecimal

import com.ing.zknotary.common.zkp.ZKProvingException
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.makeBigDecimal
import com.ing.zknotary.zinc.types.proveTimed
import com.ing.zknotary.zinc.types.setupTimed
import com.ing.zknotary.zinc.types.toWitness
import com.ing.zknotary.zinc.types.toZincJson
import com.ing.zknotary.zinc.types.verifyTimed
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BigDecimalMinusTest {
    private val log = loggerFor<BigDecimalMinusTest>()
    private val zincZKService = getZincZKService<BigDecimalMinusTest>(
        setupTimeout = Duration.ofSeconds(1800),
        provingTimeout = Duration.ofSeconds(1800),
    )

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc 0 minus 0`() {
        val zero = BigDecimal.ZERO

        val input = toWitness(zero, zero)
        val expected = zero.minus(zero).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc positive minus 0`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc negative minus 0`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc 0 minus positive`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("1.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc 0 minus negative`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("-1.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc positive minus positive (the same magnitude)`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal("1.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc negative minus negative (the same magnitude)`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal("-1.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc positive minus positive (all subtractions of digits are positive)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("1.101")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc negative minus negative (all subtractions of digits are positive)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("-1.101")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc positive minus negative (all sums of digits are less than 10)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("-1.101")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc negative minus positive (all sums of digits are less than 10)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("1.101")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc positive minus positive (result is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("2.6")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc negative minus negative (result is positive)`() {
        val left = BigDecimal("-1.8")
        val right = BigDecimal("-2.6")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc positive minus positive (one subtraction is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("0.9")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc positive minus negative (one sum is more than 10)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("-0.9")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc negative minus positive (propagate extra 1 to the end)`() {
        val left = BigDecimal("-99.9")
        val right = BigDecimal("0.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc positive minus positive (propagate deducted 1 to the end)`() {
        val left = BigDecimal("100")
        val right = BigDecimal("0.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc result exceeds the maximum value that can be stored`() {
        val left = BigDecimal("9").times(BigDecimal("10").pow(23))
        val right = BigDecimal("-9").times(BigDecimal("10").pow(23))

        val input = toWitness(left, right)

        val exception = Assertions.assertThrows(ZKProvingException::class.java) {
            zincZKService.proveTimed(input, log)
        }

        Assertions.assertTrue(
            exception.message?.contains("Magnitude exceeds the maximum stored value") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc result exceeds the minimum value that can be stored`() {
        val left = BigDecimal("-9").times(BigDecimal("10").pow(23))
        val right = BigDecimal("9").times(BigDecimal("10").pow(23))

        val input = toWitness(left, right)

        val exception = Assertions.assertThrows(ZKProvingException::class.java) {
            zincZKService.proveTimed(input, log)
        }

        Assertions.assertTrue(
            exception.message?.contains("Magnitude exceeds the maximum stored value") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of the same length (left is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of the same length (right is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - positive, right - negative, left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - positive, right - negative, right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negatives of the same length (left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negatives of the same length (right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - negative, right - positive, left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - negative, right - positive, right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positive equal numbers`() {
        val left = makeBigDecimal(byteArrayOf(-120, 34, 78, -23, -111, 45, 127, 23, 45, -3), 1)
        val right = makeBigDecimal(byteArrayOf(-120, 34, 78, -23, -111, 45, 127, 23, 45, -3), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of zero from positive`() {
        val left = makeBigDecimal(byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3), 1)
        val right = makeBigDecimal(byteArrayOf(0), 0)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of negative from zero`() {
        val left = makeBigDecimal(byteArrayOf(0), 0)
        val right = makeBigDecimal(byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of zero from zero`() {
        val left = makeBigDecimal(byteArrayOf(0), 0)
        val right = makeBigDecimal(byteArrayOf(0), 0)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of ZERO from positive`() {
        val left = makeBigDecimal(byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3), 1)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of negative from ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = makeBigDecimal(byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of ZERO from ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of ONE from ONE`() {
        val left = BigDecimal(BigInteger.ONE)
        val right = BigDecimal(BigInteger.ONE)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers so that borrow is 1`() {
        val left = makeBigDecimal(byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1), 1)
        val right = makeBigDecimal(byteArrayOf(-128, -128, -128, -128, -128, -128, -128, -128, -128), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toZincJson()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }
}
