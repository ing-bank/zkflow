package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class DoubleEqualsTest {

    companion object {
        private val circuitFolder: String = DoubleComparisonTest::class.java.getResource("/DoubleEqualsTest").path
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
    fun `zinc comparesusing equals (sign is different)`() {
        // 100
        val leftSign: Byte = 1
        val leftExponent: Short = 1
        val leftMagnitude: Long = 10

        // -100
        val rightSign: Byte = -1
        val rightExponent: Short = 1
        val rightMagnitude: Long = 10

        val input = "{\"left\": ${toZincString(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toZincString(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc comparesusing equals (exponent is different)`() {
        // 100
        val leftSign: Byte = 1
        val leftExponent: Short = 1
        val leftMagnitude: Long = 10

        // 10
        val rightSign: Byte = 1
        val rightExponent: Short = 0
        val rightMagnitude: Long = 10

        val input = "{\"left\": ${toZincString(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toZincString(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc comparesusing equals (magnitude is different)`() {
        // 100
        val leftSign: Byte = 1
        val leftExponent: Short = 1
        val leftMagnitude: Long = 10

        // 70
        val rightSign: Byte = 1
        val rightExponent: Short = 1
        val rightMagnitude: Long = 7

        val input = "{\"left\": ${toZincString(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toZincString(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    @Test
    fun `zinc comparesusing equals (both are equal to each other)`() {
        // 100
        val leftSign: Byte = 1
        val leftExponent: Short = 1
        val leftMagnitude: Long = 10

        // 100
        val rightSign: Byte = 1
        val rightExponent: Short = 1
        val rightMagnitude: Long = 10

        val input = "{\"left\": ${toZincString(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toZincString(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"0\"".toByteArray())
    }

    private fun toZincString(sign: Byte, exponent: Short, magnitude: Long) =
        "{\"exponent\": \"${exponent}\",\"magnitude\": \"${magnitude}\",\"sign\": \"${sign}\"}"
}
