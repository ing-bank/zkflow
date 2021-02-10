package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZKProvingException
import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

@Tag("slow")
class BigDecimalMinusTest {
    companion object {
        private val circuitFolder: String = BigDecimalMinusTest::class.java.getResource("/BigDecimalMinusTest").path
        private val zincZKService = ZincZKService(
            circuitFolder,
            artifactFolder = circuitFolder,
            buildTimeout = Duration.ofSeconds(5),
            setupTimeout = Duration.ofSeconds(1800),
            provingTimeout = Duration.ofSeconds(1800),
            verificationTimeout = Duration.ofSeconds(1)
        )

        @BeforeAll
        @JvmStatic
        fun setup() {
            zincZKService.setup()
        }

        @AfterAll
        @JvmStatic
        fun `remove zinc files`() {
            zincZKService.cleanup()
        }
    }

    @Test
    fun `zinc 0 minus 0`() {
        val zero = BigDecimal.ZERO

        val input = "{\"left\": ${zero.toJSON()}" +
            ",\"right\": ${zero.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, zero.minus(zero).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive minus 0`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal.ZERO

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative minus 0`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal.ZERO

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc 0 minus positive`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("1.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc 0 minus negative`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("-1.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive minus positive (the same magnitude)`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal("1.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative minus negative (the same magnitude)`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal("-1.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive minus positive (all subtractions of digits are positive)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("1.101")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative minus negative (all subtractions of digits are positive)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("-1.101")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive minus negative (all sums of digits are less than 10)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("-1.101")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative minus positive (all sums of digits are less than 10)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("1.101")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive minus positive (result is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("2.6")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative minus negative (result is positive)`() {
        val left = BigDecimal("-1.8")
        val right = BigDecimal("-2.6")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive minus positive (one subtraction is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("0.9")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive minus negative (one sum is more than 10)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("-0.9")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative minus positive (propagate extra 1 to the end)`() {
        val left = BigDecimal("-99.9")
        val right = BigDecimal("0.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive minus positive (propagate deducted 1 to the end)`() {
        val left = BigDecimal("100")
        val right = BigDecimal("0.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.minus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc result exceeds the maximum value that can be stored`() {
        val exception = assertThrows(ZKProvingException::class.java) {
            val left = BigDecimal("9").times(BigDecimal("10").pow(1023))
            val right = BigDecimal("-9").times(BigDecimal("10").pow(1023))

            val input = "{\"left\": ${left.toJSON()}" +
                ",\"right\": ${right.toJSON()}}"

            zincZKService.prove(input.toByteArray())
        }

        assertTrue(
            exception.message?.contains("Magnitude exceeds the maximum stored value") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc result exceeds the minimum value that can be stored`() {
        val exception = assertThrows(ZKProvingException::class.java) {
            val left = BigDecimal("-9").times(BigDecimal("10").pow(1023))
            val right = BigDecimal("9").times(BigDecimal("10").pow(1023))

            val input = "{\"left\": ${left.toJSON()}" +
                ",\"right\": ${right.toJSON()}}"

            zincZKService.prove(input.toByteArray())
        }

        assertTrue(
            exception.message?.contains("Magnitude exceeds the maximum stored value") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two of the same positive scale`() {
        val leftString = "1231212478987482988429808779810457634781384756794987"
        val leftScale = 10
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "747233429293018787918347987234564568"
        val rightScale = 10
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two of the same negative scale`() {
        val leftString = "1231212478987482988429808779810457634781384756794987"
        val leftScale = -10
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "747233429293018787918347987234564568"
        val rightScale = -10
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two with different scales (left - positive, right - negative)`() {
        val leftString = "1231212478987482988429808779810457634781384756794987"
        val leftScale = 15
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "747233429293018787918347987234564568"
        val rightScale = -10
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc subtraction of two with different scales (left - negative, right -  positive)`() {
        val leftString = "0"
        val leftScale = -15
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "0"
        val rightScale = 10
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of the same length (left is greater)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of the same length (right is greater)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - positive, right - negative, left abs value is greater)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - positive, right - negative, right abs value is greater)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negatives of the same length (left abs value is greater)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negatives of the same length (right abs value is greater)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - negative, right - positive, left abs value is greater)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different signs (left - negative, right - positive, right abs value is greater)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of different length (left is longer)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positives of different length (right is longer)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of different length (left - positive, right - negative, left abs value is greater)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers of the same length (left - positive, right - negative, right abs value is greater)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    /**
     * Subtract two numbers of different length and different signs.
     * The first is negative.
     * The first is longer.
     */
    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers (left - negative, right - positive, left is longer)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negative numbers (left is longer)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two negative numbers (right is longer)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two positive equal numbers`() {
        val leftBytes = byteArrayOf(-120, 34, 78, -23, -111, 45, 127, 23, 45, -3)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(-120, 34, 78, -23, -111, 45, 127, 23, 45, -3)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of zero from positive`() {
        val leftBytes = byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(0)
        val rightSign = 0
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of negative from zero`() {
        val leftBytes = byteArrayOf(0)
        val leftSign = 0
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of zero from zero`() {
        val leftBytes = byteArrayOf(0)
        val leftSign = 0
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(0)
        val rightSign = 0
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of ZERO from positive`() {
        val leftBytes = byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val right = BigDecimal(BigInteger.ZERO)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of negative from ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)

        val rightBytes = byteArrayOf(120, 34, 78, -23, -111, 45, 127, 23, 45, -3)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of ZERO from ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of ONE from ONE`() {
        val left = BigDecimal(BigInteger.ONE)
        val right = BigDecimal(BigInteger.ONE)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc subtraction of two numbers so that borrow is 1`() {
        val leftBytes = byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(-128, -128, -128, -128, -128, -128, -128, -128, -128)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.subtract(right).toJSON().toByteArray())
    }
}
