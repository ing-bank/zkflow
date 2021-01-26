package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class DoubleComparisonTest {

    companion object {
        private val circuitFolder: String = DoubleComparisonTest::class.java.getResource("/DoubleComparisonTest").path
        private val sharedModules: Array<String> = arrayOf(DoubleComparisonTest::class.java.getResource("/shared/floating_point.zn").path.toString())
        private val zincZKService = ZincZKService(
            circuitFolder,
            artifactFolder = circuitFolder,
            buildTimeout = Duration.ofSeconds(5),
            setupTimeout = Duration.ofSeconds(300),
            provingTimeout = Duration.ofSeconds(300),
            verificationTimeout = Duration.ofSeconds(1)
        )

        @BeforeAll
        @JvmStatic
        fun setup() {
            composeTempCircuit(circuitFolder, sharedModules)
            zincZKService.setup()
        }

        @AfterAll
        @JvmStatic
        fun `remove zinc files`() {
            zincZKService.cleanup()
        }
    }

    @Test
    fun `zinc compares two zeros`() {
        // 0
        val leftSign: Byte = 0
        val leftExponent: Short = 0
        val leftMagnitude: Long = 0

        // 0
        val rightSign: Byte = 0
        val rightExponent: Short = 0
        val rightMagnitude: Long = 0

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"0\"".toByteArray())
    }

    @Test
    fun `zinc compares positive without exponent with zero`() {
        // 10
        val leftSign: Byte = 1
        val leftExponent: Short = 0
        val leftMagnitude: Long = 10

        // 0
        val rightSign: Byte = 0
        val rightExponent: Short = 0
        val rightMagnitude: Long = 0

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares negative without exponent with zero`() {
        // -10
        val leftSign: Byte = -1
        val leftExponent: Short = 0
        val leftMagnitude: Long = 10

        // 0
        val rightSign: Byte = 0
        val rightExponent: Short = 0
        val rightMagnitude: Long = 0

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives with exponents with diff less than 16 (left exponent is bigger)`() {
        // 2E+10
        val leftSign: Byte = 1
        val leftExponent: Short = 10
        val leftMagnitude: Long = 2

        // 1.2345E+10
        val rightSign: Byte = 1
        val rightExponent: Short = 5
        val rightMagnitude: Long = 123456

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives with exponents with diff less than 16 (right exponent is bigger)`() {
        // 1.2345E+10
        val leftSign: Byte = 1
        val leftExponent: Short = 5
        val leftMagnitude: Long = 123456

        // 2E+10
        val rightSign: Byte = 1
        val rightExponent: Short = 10
        val rightMagnitude: Long = 2

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two negatives with exponents with diff less than 16 (left exponent is bigger)`() {
        // -2E+10
        val leftSign: Byte = -1
        val leftExponent: Short = 10
        val leftMagnitude: Long = 2

        // -1.2345E+10
        val rightSign: Byte = -1
        val rightExponent: Short = 5
        val rightMagnitude: Long = 123456

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two negatives with exponents with diff less than 16 (right exponent is bigger)`() {
        // -1.2345E+10
        val leftSign: Byte = -1
        val leftExponent: Short = 5
        val leftMagnitude: Long = 123456

        // -2E+10
        val rightSign: Byte = -1
        val rightExponent: Short = 10
        val rightMagnitude: Long = 2

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives with exponents with diff not less than 16 (left exponent is bigger)`() {
        // 2E+10
        val leftSign: Byte = 1
        val leftExponent: Short = 21
        val leftMagnitude: Long = 2

        // 1.2345E+10
        val rightSign: Byte = 1
        val rightExponent: Short = 5
        val rightMagnitude: Long = 123456

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives with exponents with diff not less than 16 (right exponent is bigger)`() {
        // 1.2345E+10
        val leftSign: Byte = 1
        val leftExponent: Short = 5
        val leftMagnitude: Long = 123456

        // 2E+10
        val rightSign: Byte = 1
        val rightExponent: Short = 21
        val rightMagnitude: Long = 2

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two negatives with exponents with diff not less than 16 (left exponent is bigger)`() {
        // -2E+21
        val leftSign: Byte = -1
        val leftExponent: Short = 21
        val leftMagnitude: Long = 2

        // -1.2345E+10
        val rightSign: Byte = -1
        val rightExponent: Short = 5
        val rightMagnitude: Long = 123456

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two negatives with exponents with diff not less than 16 (right exponent is bigger)`() {
        // -1.2345E+10
        val leftSign: Byte = -1
        val leftExponent: Short = 5
        val leftMagnitude: Long = 123456

        // -2E+21
        val rightSign: Byte = -1
        val rightExponent: Short = 21
        val rightMagnitude: Long = 2

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (left is greater)`() {
        // 11
        val leftSign: Byte = 1
        val leftExponent: Short = 0
        val leftMagnitude: Long = 11

        // 10
        val rightSign: Byte = 1
        val rightExponent: Short = 0
        val rightMagnitude: Long = 10

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (right is greater)`() {
        // 10
        val leftSign: Byte = 1
        val leftExponent: Short = 0
        val leftMagnitude: Long = 10

        // 11
        val rightSign: Byte = 1
        val rightExponent: Short = 0
        val rightMagnitude: Long = 11

        val input = "{\"left\": ${toDoubleJSON(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toDoubleJSON(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }
}
