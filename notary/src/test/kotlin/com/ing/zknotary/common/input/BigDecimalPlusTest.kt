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
class BigDecimalPlusTest {
    companion object {
        private val circuitFolder: String = BigDecimalPlusTest::class.java.getResource("/BigDecimalPlusTest").path
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
    fun `zinc 0 plus 0`() {
        val zero = BigDecimal.ZERO

        val input = "{\"left\": ${zero.toJSON()}" +
            ",\"right\": ${zero.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, zero.toJSON().toByteArray())
    }

    @Test
    fun `zinc positive plus 0`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal.ZERO

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative plus 0`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal.ZERO

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc 0 plus positive`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("1.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc 0 plus negative`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal("-1.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive plus negative (the same magnitude)`() {
        val left = BigDecimal("1.1")
        val right = BigDecimal("-1.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative plus positive (the same magnitude)`() {
        val left = BigDecimal("-1.1")
        val right = BigDecimal("1.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive plus positive (all sums of digits are less than 10)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("1.101")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative plus negative (all subtractions of digits are positive)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("-1.101")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive plus negative (all sums of digits are less than 10)`() {
        val left = BigDecimal("2.222")
        val right = BigDecimal("-1.101")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative plus positive (all sums of digits are less than 10)`() {
        val left = BigDecimal("-2.222")
        val right = BigDecimal("1.101")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive plus negative (result is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("-2.6")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative plus positive (result is positive)`() {
        val left = BigDecimal("-1.8")
        val right = BigDecimal("2.6")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive plus negative (one subtraction is negative)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("-0.9")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive plus positive (one sum is more than 10)`() {
        val left = BigDecimal("1.8")
        val right = BigDecimal("0.9")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc negative plus negative (propagate extra 1 to the end)`() {
        val left = BigDecimal("-99.9")
        val right = BigDecimal("-0.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc positive plus negative (propagate deducted 1 to the end)`() {
        val left = BigDecimal("100")
        val right = BigDecimal("-0.1")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `zinc result exceeds the maximum value that can be stored`() {
        val exception = assertThrows(ZKProvingException::class.java) {
            val left = BigDecimal("9").times(BigDecimal("10").pow(1023))
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
    fun `zinc result exceeds the minimum value that can be stored`() {
        val exception = assertThrows(ZKProvingException::class.java) {
            val left = BigDecimal("-9").times(BigDecimal("10").pow(1023))
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
    fun `BigDecimal compatibility - zinc sum of two of the same positive scale`() {
        val leftString = "1231212478987482988429808779810457634781384756794987"
        val leftScale = 10
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "747233429293018787918347987234564568"
        val rightScale = 10
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc sum of two of the same negative scale`() {
        val leftString = "1231212478987482988429808779810457634781384756794987"
        val leftScale = -10
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "747233429293018787918347987234564568"
        val rightScale = -10
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc sum of two with different scales (first - positive, second negative)`() {
        val leftString = "1231212478987482988429808779810457634781384756794987"
        val leftScale = 15
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "747233429293018787918347987234564568"
        val rightScale = -10
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc sum of two with different scales (left - negative, right - positive)`() {
        val leftString = "1231212478987482988429808779810457634781384756794987"
        val leftScale = -15
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "747233429293018787918347987234564568"
        val rightScale = 10
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `BigDecimal compatibility - zinc sum of two zeroes with different scales (left - negative, right - positive)`() {
        val leftString = "0"
        val leftScale = -15
        val left = BigDecimal(BigInteger(leftString), leftScale)

        val rightString = "0"
        val rightScale = 10
        val right = BigDecimal(BigInteger(rightString), rightScale)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.plus(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two positives of the same length`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val bSign = 1
        val right = BigDecimal(BigInteger(bSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two positives of the same length 2`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers 0f the same length (left - positive, right - negative, left abs value is greater)`() {
        val leftBytes = byteArrayOf(3, 4, 5, 6, 7, 8, 9)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of the same length (left - negative, right - positive, left abs value is greater)`() {
        val leftBytes = byteArrayOf(3, 4, 5, 6, 7, 8, 9)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of the same length (left - positive, right - negative, right abs value is greater)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(3, 4, 5, 6, 7, 8, 9)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of the same length (left - negative, right - positive, right abs value is greater)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(3, 4, 5, 6, 7, 8, 9)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two positives of different length (left is longer)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two positives of different length (right is longer)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val left = BigDecimal(BigInteger(leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val right = BigDecimal(BigInteger(rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two negatives of different length (left is longer)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two negatives of different length (right is longer)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of different length (left - positive, right - negative, left - longer)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of different length (left - positive, right - negative, right - longer)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val rightSign = -1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of different length (left - negative, right - positive, left - longer)`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers of different length (left - negative, right - positive, right - longer)`() {
        val leftBytes = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 10, 20, 30)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two equal numbers with different signs`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val leftSign = -1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum number and zero`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(0)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum zero and number`() {
        val leftBytes = byteArrayOf(0)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum zero and zero`() {
        val leftBytes = byteArrayOf(0)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(0)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum number and ZERO`() {
        val leftBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val right = BigDecimal(BigInteger.ZERO)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum ZERO and number`() {
        val left = BigDecimal(BigInteger.ZERO)

        val rightBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum ZERO and ZERO`() {
        val left = BigDecimal(BigInteger.ZERO)
        val right = BigDecimal(BigInteger.ZERO)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum ONE and ONE`() {
        val left = BigDecimal(BigInteger.ONE)
        val right = BigDecimal(BigInteger.ONE)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }

    @Test
    fun `BigInteger compatibility - zinc sum two numbers so that carry is 1`() {
        val leftBytes = byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1)
        val leftSign = 1
        val left = BigDecimal(BigInteger(leftSign, leftBytes))

        val rightBytes = byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1)
        val rightSign = 1
        val right = BigDecimal(BigInteger(rightSign, rightBytes))

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, left.add(right).toJSON().toByteArray())
    }
}
