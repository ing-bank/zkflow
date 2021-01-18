package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class BigDecimalComparisonTest {
    companion object {
        private val circuitFolder: String = DoubleComparisonTest::class.java.getResource("/BigDecimalComparisonTest").path
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
            zincZKService.setup()
        }

        @AfterAll
        @JvmStatic
        fun `remove zinc files`() {
            zincZKService.cleanup()
        }
    }

    @Test
    fun `zinc compares two positives (left sign is greater)`() {
        // 1.1
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[0] = 1
        val leftFraction = ByteArray(128)
        leftFraction[127] = 1

        // -1.1
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        rightInteger[0] = 1
        val rightFraction = ByteArray(128)
        rightFraction[127] = 1

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (right sign is greater)`() {
        // -1.1
        val leftSign: Byte = -1
        val leftInteger = ByteArray(1024)
        leftInteger[0] = 1
        val leftFraction = ByteArray(128)
        leftFraction[127] = 1

        // 1.1
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[0] = 1
        val rightFraction = ByteArray(128)
        rightFraction[127] = 1

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (left integer is greater)`() {
        // 0.1
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[0] = 1
        val leftFraction = ByteArray(128)

        // 0
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (right integer is greater)`() {
        // 0
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        val leftFraction = ByteArray(128)

        // 1
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[0] = 1
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (left fraction is greater)`() {
        // 0.1
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        val leftFraction = ByteArray(128)
        leftFraction[127] = 1

        // 0
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (right fraction is greater)`() {
        // 0
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        val leftFraction = ByteArray(128)

        // 0.1
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)
        rightFraction[127] = 1

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two zeros`() {
        // 0
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        val leftFraction = ByteArray(128)

        // 0
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toZincString(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toZincString(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"0\"".toByteArray())
    }

    private fun toZincString(sign: Byte, integer: ByteArray, fraction: ByteArray) =
        "{\"sign\": \"$sign\", \"integer\": [${integer.joinToString { "\"${it}\"" }}], \"fraction\": [${fraction.joinToString { "\"${it}\"" }}]}"
}
