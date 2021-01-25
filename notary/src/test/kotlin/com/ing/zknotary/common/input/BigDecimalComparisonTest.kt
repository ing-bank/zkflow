package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class BigDecimalComparisonTest {
    companion object {
        private val circuitFolder: String = BigDecimalComparisonTest::class.java.getResource("/BigDecimalComparisonTest").path
        private val sharedModules: Array<String> = arrayOf(BigDecimalComparisonTest::class.java.getResource("/shared/big_decimal.zn").path.toString())
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
    fun `zinc compares two positives (left sign is greater)`() {
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

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (right sign is greater)`() {
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

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (left integer is greater)`() {
        // 0.1
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 1
        val leftFraction = ByteArray(128)

        // 0
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

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
        rightInteger[1023] = 1
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two positives (left fraction is greater)`() {
        // 0.1
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        val leftFraction = ByteArray(128)
        leftFraction[0] = 1

        // 0
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

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
        rightFraction[0] = 1

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"-1\"".toByteArray())
    }

    @Test
    fun `zinc compares two number of the same digits, but in opposite order (smoke test for endian order)`() {
        // 123
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[1023] = 3
        leftInteger[1022] = 2
        leftInteger[1021] = 1
        val leftFraction = ByteArray(128)

        // 321
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[1023] = 1
        rightInteger[1022] = 2
        rightInteger[1021] = 3
        val rightFraction = ByteArray(128)

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

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

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"0\"".toByteArray())
    }
}
