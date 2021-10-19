package com.ing.zkflow.zinc.types.java.bigdecimal

import com.ing.zkflow.common.zkp.ZKProvingException
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.testing.zkp.proveTimed
import com.ing.zkflow.testing.zkp.setupTimed
import com.ing.zkflow.testing.zkp.verifyTimed
import com.ing.zkflow.zinc.types.makeBigDecimal
import com.ing.zkflow.zinc.types.toWitness
import com.ing.zkflow.zinc.types.toZincJson
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

class BigDecimalPlusTest {
    private val zincZKService = getZincZKService<BigDecimalPlusTest>(
        setupTimeout = Duration.ofSeconds(1800),
        provingTimeout = Duration.ofSeconds(1800),
    )

    init {
        zincZKService.setupTimed()
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc 0 plus 0`() {
        val zero = BigDecimal.ZERO

        val input = toWitness(zero, zero)
        val expected = zero.toZincJson()

        val proof = zincZKService.proveTimed(input)
        zincZKService.verifyTimed(proof, expected)
    }

    @Test
    fun `zinc positive plus 0`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc negative plus 0`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc 0 plus positive`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("1.1")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc 0 plus negative`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("-1.1")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc positive plus negative (the same magnitude)`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal("-1.1")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc negative plus positive (the same magnitude)`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal("1.1")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc positive plus positive (all sums of digits are less than 10)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("1.101")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc negative plus negative (all subtractions of digits are positive)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("-1.101")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc positive plus negative (all sums of digits are less than 10)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("-1.101")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc negative plus positive (all sums of digits are less than 10)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("1.101")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc positive plus negative (result is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("-2.6")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc negative plus positive (result is positive)`() {
        val left = BigDecimal("-1.8")
        val right = BigDecimal("2.6")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc positive plus negative (one subtraction is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("-0.9")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc positive plus positive (one sum is more than 10)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("0.9")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc negative plus negative (propagate extra 1 to the end)`() {
        val left = BigDecimal("-99.9")
        val right = BigDecimal("-0.1")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc positive plus negative (propagate deducted 1 to the end)`() {
        val left = BigDecimal("100")
        val right = BigDecimal("-0.1")

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc result exceeds the maximum value that can be stored`() {
        val left = BigDecimal("9").times(BigDecimal("10").pow(23))
        val right = BigDecimal("9").times(BigDecimal("10").pow(23))

        val input = toWitness(left, right)

        val exception = assertThrows(ZKProvingException::class.java) {
            zincZKService.proveTimed(input)
        }

        assertTrue(
            exception.message?.contains("Magnitude exceeds the maximum stored value") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc result exceeds the minimum value that can be stored`() {
        val left = BigDecimal("-9").times(BigDecimal("10").pow(23))
        val right = BigDecimal("-9").times(BigDecimal("10").pow(23))

        val input = toWitness(left, right)

        val exception = assertThrows(ZKProvingException::class.java) {
            zincZKService.proveTimed(input)
        }

        assertTrue(
            exception.message?.contains("Magnitude exceeds the maximum stored value") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `BigInteger compatibility - zinc sum two positives of the same length`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two positives of the same length 2`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers 0f the same length (left - positive, right - negative, left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(3, 4, 5, 6, 7, 8, 9), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), -1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of the same length (left - negative, right - positive, left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(3, 4, 5, 6, 7, 8, 9), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of the same length (left - positive, right - negative, right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(3, 4, 5, 6, 7, 8, 9), -1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of the same length (left - negative, right - positive, right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(3, 4, 5, 6, 7, 8, 9), 1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum two equal numbers with different signs`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum number and zero`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(0), 1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum zero and number`() {
        val left = makeBigDecimal(byteArrayOf(0), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum zero and zero`() {
        val left = makeBigDecimal(byteArrayOf(0), 1)
        val right = makeBigDecimal(byteArrayOf(0), 1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum number and ZERO`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 1)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum ZERO and number`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 1)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum ZERO and ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `BigInteger compatibility - zinc sum ONE and ONE`() {
        val left = BigDecimal(BigInteger.ONE)
        val right = BigDecimal(BigInteger.ONE)

        val input = toWitness(left, right)
        val expected = left.plus(right).toZincJson()

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }
}
