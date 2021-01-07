package com.ing.zknotary.common.hashes

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Duration

class DoubleComparisonTest {
    private val circuitFolder: String = javaClass.getResource("/DoubleComparisonTest").path
    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
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
    fun `zinc verifies double comparison`() {
        // -10
        val leftSign: Byte = -1
        val leftExponent: Short = 0
        val leftMagnitude: Long = 10

        // -11
        val rightSign: Byte = -1
        val rightExponent: Short = 0
        val rightMagnitude: Long = 11

        val input = "{\"left\": ${toZincString(leftSign, leftExponent, leftMagnitude)}" +
            ",\"right\": ${toZincString(rightSign, rightExponent, rightMagnitude)}}"

        val output = zincZKService.prove(input.toByteArray())
        zincZKService.verify(output, "\"1\"".toByteArray())
    }

    private fun toZincString(sign: Byte, exponent: Short, magnitude: Long) =
        "{\"exponent\": \"${exponent}\",\"magnitude\": \"${magnitude}\",\"sign\": \"${sign}\"}"
}
