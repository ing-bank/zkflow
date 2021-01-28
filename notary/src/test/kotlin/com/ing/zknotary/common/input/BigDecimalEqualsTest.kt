package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class BigDecimalEqualsTest {
    companion object {
        private val circuitFolder: String = BigDecimalEqualsTest::class.java.getResource("/BigDecimalEqualsTest").path
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
    fun `zinc compares using equals (sign is different)`() {
        // 421.42
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[2] = 4
        leftInteger[1] = 2
        leftInteger[0] = 1
        val leftFraction = ByteArray(128)
        leftFraction[127] = 4
        leftFraction[126] = 2

        // -421.42
        val rightSign: Byte = -1
        val rightInteger = ByteArray(1024)
        rightInteger[2] = 4
        rightInteger[1] = 2
        rightInteger[0] = 1
        val rightFraction = ByteArray(128)
        rightFraction[127] = 4
        rightFraction[126] = 2

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares using equals (integer is different)`() {
        // 421.42
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[2] = 4
        leftInteger[1] = 2
        leftInteger[0] = 1
        val leftFraction = ByteArray(128)
        leftFraction[127] = 4
        leftFraction[126] = 2

        // 42.42
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[1] = 4
        rightInteger[0] = 2
        val rightFraction = ByteArray(128)
        rightFraction[127] = 4
        rightFraction[126] = 2

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares using equals (fraction is different)`() {
        // 421.42
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[2] = 4
        leftInteger[1] = 2
        leftInteger[0] = 1
        val leftFraction = ByteArray(128)
        leftFraction[127] = 4
        leftFraction[126] = 2

        // 421.421
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[2] = 4
        rightInteger[1] = 2
        rightInteger[0] = 1
        val rightFraction = ByteArray(128)
        rightFraction[127] = 4
        rightFraction[126] = 2
        rightFraction[125] = 1

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc compares using equals (both are equal to each other)`() {
        // 421.42
        val leftSign: Byte = 1
        val leftInteger = ByteArray(1024)
        leftInteger[2] = 4
        leftInteger[1] = 2
        leftInteger[0] = 1
        val leftFraction = ByteArray(128)
        leftFraction[127] = 4
        leftFraction[126] = 2

        // 421.42
        val rightSign: Byte = 1
        val rightInteger = ByteArray(1024)
        rightInteger[2] = 4
        rightInteger[1] = 2
        rightInteger[0] = 1
        val rightFraction = ByteArray(128)
        rightFraction[127] = 4
        rightFraction[126] = 2

        val input = "{\"left\": ${toBigDecimalJSON(leftSign, leftInteger, leftFraction)}" +
            ",\"right\": ${toBigDecimalJSON(rightSign, rightInteger, rightFraction)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"0\"".toByteArray())
    }
}
