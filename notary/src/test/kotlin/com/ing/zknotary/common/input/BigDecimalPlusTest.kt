package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZKProvingException
import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
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
        // 0
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        val leftFraction = ByteArray(128)

        // 0
        val rightSign: Byte = 0
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(leftSign, leftInteger, leftFraction).toByteArray())
    }

    @Test
    fun `zinc positive plus 0`() {
        // 1.1
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        leftFraction[0] = 1

        // 0
        val rightSign: Byte = 0
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(leftSign, leftInteger, leftFraction).toByteArray())
    }

    @Test
    fun `zinc negative plus 0`() {
        // -1.1
        val leftSign: Byte = -1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        leftFraction[0] = 1

        // 0
        val rightSign: Byte = 0
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(leftSign, leftInteger, leftFraction).toByteArray())
    }

    @Test
    fun `zinc 0 plus positive`() {
        // 0
        val leftSign: Byte = 0
        val leftInteger = ByteArray(1024)
        val leftFraction = ByteArray(128)

        // 1.1
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(rightSign, rightInteger, rightFraction).toByteArray())
    }

    @Test
    fun `zinc 0 plus negative`() {
        // 0
        val leftSign: Byte = 0
        val leftInteger = ByteArray(1024)
        val leftFraction = ByteArray(128)

        // -1.1
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(rightSign, rightInteger, rightFraction).toByteArray())
    }

    @Test
    fun `zinc positive plus negative (the same magnitude)`() {
        // 1.1
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        leftFraction[0] = 1

        // -1.1
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(0, ByteArray(1024), ByteArray(128)).toByteArray())
    }

    @Test
    fun `zinc negative plus positive (the same magnitude)`() {
        // -1.1
        val leftSign: Byte = -1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        leftFraction[0] = 1

        // 1.1
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(0, ByteArray(1024), ByteArray(128)).toByteArray())
    }

    @Test
    fun `zinc positive plus positive (all sums of digits are less than 10)`() {
        // 2.222
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 2
        val leftFraction = ByteArray(128)
        leftFraction[0] = 2
        leftFraction[1] = 2
        leftFraction[2] = 2

        // 1.101
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1
        rightFraction[2] = 1

        // 3.323
        val sign: Byte = 1
        val integer = ByteArray(1024)
        integer[1023] = 3
        val fraction = ByteArray(128)
        fraction[0] = 3
        fraction[1] = 2
        fraction[2] = 3

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc negative plus negative (all subtractions of digits are positive)`() {
        // -2.222
        val leftSign: Byte = -1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 2
        val leftFraction = ByteArray(128)
        leftFraction[0] = 2
        leftFraction[1] = 2
        leftFraction[2] = 2

        // -1.101
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1
        rightFraction[2] = 1

        // -3.323
        val sign: Byte = -1
        val integer = ByteArray(1024)
        integer[1023] = 3
        val fraction = ByteArray(128)
        fraction[0] = 3
        fraction[1] = 2
        fraction[2] = 3

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc positive plus negative (all sums of digits are less than 10)`() {
        // 2.222
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 2
        val leftFraction = ByteArray(128)
        leftFraction[0] = 2
        leftFraction[1] = 2
        leftFraction[2] = 2

        // -1.101
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1
        rightFraction[2] = 1

        // 1.121
        val sign: Byte = 1
        val integer = ByteArray(1024)
        integer[1023] = 1
        val fraction = ByteArray(128)
        fraction[0] = 1
        fraction[1] = 2
        fraction[2] = 1

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc negative plus positive (all sums of digits are less than 10)`() {
        // -2.222
        val leftSign: Byte = -1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 2
        val leftFraction = ByteArray(128)
        leftFraction[0] = 2
        leftFraction[1] = 2
        leftFraction[2] = 2

        // 1.101
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1
        rightFraction[2] = 1

        // -1.121
        val sign: Byte = -1
        val integer = ByteArray(1024)
        integer[1023] = 1
        val fraction = ByteArray(128)
        fraction[0] = 1
        fraction[1] = 2
        fraction[2] = 1

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc positive plus negative (result is negative)`() {
        // 1.8
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        leftFraction[0] = 8

        // -2.6
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 2
        val rightFraction = ByteArray(128)
        rightFraction[0] = 6

        // -0.8
        val sign: Byte = -1
        val integer = ByteArray(1024)
        val fraction = ByteArray(128)
        fraction[0] = 8

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc negative plus positive (result is positive)`() {
        // -1.8
        val leftSign: Byte = -1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        leftFraction[0] = 8

        // 2.6
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 2
        val rightFraction = ByteArray(128)
        rightFraction[0] = 6

        // 0.8
        val sign: Byte = 1
        val integer = ByteArray(1024)
        val fraction = ByteArray(128)
        fraction[0] = 8

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc positive plus negative (one subtraction is negative)`() {
        // 1.8
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        leftFraction[0] = 8

        // -0.9
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)
        rightFraction[0] = 9

        // 0.9
        val sign: Byte = 1
        val integer = ByteArray(1024)
        val fraction = ByteArray(128)
        fraction[0] = 9

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc positive plus positive (one sum is more than 10)`() {
        // 1.8
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)
        leftFraction[0] = 8

        // -0.9
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)
        rightFraction[0] = 9

        // 2.7
        val sign: Byte = 1
        val integer = ByteArray(1024)
        integer[1023] = 2
        val fraction = ByteArray(128)
        fraction[0] = 7

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc negative plus negative (propagate extra 1 to the end)`() {
        // -99.9
        val leftSign: Byte = -1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 9
        leftInteger[1022] = 9
        val leftFraction = ByteArray(128)
        leftFraction[0] = 9

        // -0.1
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1

        // -100
        val sign: Byte = -1
        val integer = ByteArray(1024)
        integer[1023] = 0
        integer[1022] = 0
        integer[1021] = 1
        val fraction = ByteArray(128)

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc positive plus negative (propagate deducted 1 to the end)`() {
        // 100
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 0
        leftInteger[1022] = 0
        leftInteger[1021] = 1
        val leftFraction = ByteArray(128)

        // -0.1
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)
        rightFraction[0] = 1

        // 99.9
        val sign: Byte = 1
        val integer = ByteArray(1024)
        integer[1023] = 9
        integer[1022] = 9
        val fraction = ByteArray(128)
        fraction[0] = 9

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, toZincString(sign, integer, fraction).toByteArray())
    }

    @Test
    fun `zinc result exceeds the maximum value that can be stored`() {
        val exception = assertThrows(ZKProvingException::class.java) {
            // 9E+1022
            val leftSign: Byte = 1
            val leftInteger = ByteArray(1024)
            leftInteger[0] = 9
            val leftFraction = ByteArray(128)

            // 9E+1022
            val rightSign: Byte = 1
            val rightInteger = ByteArray(1024)
            rightInteger[0] = 9
            val rightFraction = ByteArray(128)

            val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
                ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

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
            // -9E+1022
            val leftSign: Byte = -1
            val leftInteger = ByteArray(1024)
            leftInteger[0] = 9
            val leftFraction = ByteArray(128)

            // -9E+1022
            val rightSign: Byte = -1
            val rightInteger = ByteArray(1024)
            rightInteger[0] = 9
            val rightFraction = ByteArray(128)

            val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
                ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

            zincZKService.prove(input.toByteArray())
        }

        assertTrue(
            exception.message?.contains("Magnitude exceeds the maximum stored value") ?: false,
            "Circuit fails with different error"
        )
    }

    private fun toZincString(sign: Byte, integer: ByteArray, fraction: ByteArray) =
        "{\"sign\": \"$sign\", \"integer\": [${integer.joinToString { "\"${it}\"" }}], \"fraction\": [${fraction.joinToString { "\"${it}\"" }}]}"
}
