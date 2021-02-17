package com.ing.zknotary.common.zinc.types

import com.ing.zknotary.common.zkp.ZKProvingException
import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

@Tag("slow")
class BigDecimalMinusTest {
    private val circuitFolder: String = BigDecimalMinusTest::class.java.getResource("/BigDecimalMinusTest").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(1800),
        provingTimeout = Duration.ofSeconds(1800),
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
    fun `zinc 0 minus 0`() {
        val zero = BigDecimal.ZERO

        val input = toWitness(zero, zero)
        val expected = zero.minus(zero).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc positive minus 0`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc negative minus 0`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal.ZERO

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc 0 minus positive`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("1.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc 0 minus negative`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("-1.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc positive minus positive (the same magnitude)`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal("1.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc negative minus negative (the same magnitude)`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal("-1.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc positive minus positive (all subtractions of digits are positive)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("1.101")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc negative minus negative (all subtractions of digits are positive)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("-1.101")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc positive minus negative (all sums of digits are less than 10)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("-1.101")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc negative minus positive (all sums of digits are less than 10)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("1.101")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc positive minus positive (result is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("2.6")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc negative minus negative (result is positive)`() {
        val left = BigDecimal("-1.8")
        val right = BigDecimal("-2.6")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc positive minus positive (one subtraction is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("0.9")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc positive minus negative (one sum is more than 10)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("-0.9")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc negative minus positive (propagate extra 1 to the end)`() {
        val left = BigDecimal("-99.9")
        val right = BigDecimal("0.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc positive minus positive (propagate deducted 1 to the end)`() {
        val left = BigDecimal("100")
        val right = BigDecimal("0.1")

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `zinc result exceeds the maximum value that can be stored`() {
        val left = BigDecimal("9").times(BigDecimal("10").pow(1023))
        val right = BigDecimal("-9").times(BigDecimal("10").pow(1023))

        val input = toWitness(left, right)

        val exception = assertThrows(ZKProvingException::class.java) {
            zincZKService.prove(input.toByteArray())
        }

        assertTrue(
            exception.message?.contains("Magnitude exceeds the maximum stored value") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc result exceeds the minimum value that can be stored`() {
        val left = BigDecimal("-9").times(BigDecimal("10").pow(1023))
        val right = BigDecimal("9").times(BigDecimal("10").pow(1023))

        val input = toWitness(left, right)

        val exception = assertThrows(ZKProvingException::class.java) {
            zincZKService.prove(input.toByteArray())
        }

        assertTrue(
            exception.message?.contains("Magnitude exceeds the maximum stored value") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two of the same positive scale`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", 10)
        val right = makeBigDecimal("747233429293018787918347987234564568", 10)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two of the same negative scale`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", -10)
        val right = makeBigDecimal("747233429293018787918347987234564568", -10)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two with different scales (left - positive, right - negative)`() {
        val left = makeBigDecimal("1231212478987482988429808779810457634781384756794987", 15)
        val right = makeBigDecimal("747233429293018787918347987234564568", -10)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two with different scales (left - negative, right -  positive)`() {
        val left = makeBigDecimal("0", -15)
        val right = makeBigDecimal("0", 10)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of the same length (left is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of the same length (right is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - positive, right - negative, left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - positive, right - negative, right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negatives of the same length (left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negatives of the same length (right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - negative, right - positive, left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - negative, right - positive, right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of different length (left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of different length (right is longer)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different length (left - positive, right - negative, left abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), 1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of the same length (left - positive, right - negative, right abs value is greater)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers (left - negative, right - positive, left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negative numbers (left is longer)`() {
        val left = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)
        val right = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negative numbers (right is longer)`() {
        val left = makeBigDecimal(byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30), -1)
        val right = makeBigDecimal(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positive equal numbers`() {
        val left = makeBigDecimal(byteArrayOf(-120, 34, 78, -23, -111, 45, 127, 23, 45, -3), 1)
        val right = makeBigDecimal(byteArrayOf(-120, 34, 78, -23, -111, 45, 127, 23, 45, -3), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of zero from positive`() {
        val left = makeBigDecimal(byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3), 1)
        val right = makeBigDecimal(byteArrayOf(0), 0)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of negative from zero`() {
        val left = makeBigDecimal(byteArrayOf(0), 0)
        val right = makeBigDecimal(byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of zero from zero`() {
        val left = makeBigDecimal(byteArrayOf(0), 0)
        val right = makeBigDecimal(byteArrayOf(0), 0)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of ZERO from positive`() {
        val left = makeBigDecimal(byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3), 1)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of negative from ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = makeBigDecimal(byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3), -1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of ZERO from ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of ONE from ONE`() {
        val left = BigDecimal(BigInteger.ONE)
        val right = BigDecimal(BigInteger.ONE)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers so that borrow is 1`() {
        val left = makeBigDecimal(byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1), 1)
        val right = makeBigDecimal(byteArrayOf(-128, -128, -128, -128, -128, -128, -128, -128, -128), 1)

        val input = toWitness(left, right)
        val expected = left.minus(right).toJSON()

        zincZKService.prove(input.toByteArray()).let {
            zincZKService.verify(it, expected.toByteArray())
        }
    }
}
