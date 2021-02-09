package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration

class BigDecimalComparisonTest {
    companion object {
        private val circuitFolder: String = BigDecimalComparisonTest::class.java.getResource("/BigDecimalComparisonTest").path
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
        val left = BigDecimal(1.1)
        val right = BigDecimal(-1.1)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (right sign is greater)`() {
        val left = BigDecimal(-1.1)
        val right = BigDecimal(1.1)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (left integer is greater)`() {
        val left = BigDecimal.ONE
        val right = BigDecimal.ZERO

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (right integer is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal.ONE

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (left fraction is greater)`() {
        val left = BigDecimal(0.1)
        val right = BigDecimal.ZERO

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two positives (right fraction is greater)`() {
        val left = BigDecimal.ZERO
        val right = BigDecimal(0.1)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two number of the same digits, but in opposite order (smoke test for big-endianness)`() {
        val left = BigDecimal("123")
        val right = BigDecimal("321")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${left.compareTo(right)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares two zeros`() {
        val zero = BigDecimal.ZERO

        val input = "{\"left\": ${zero.toJSON()}" +
            ",\"right\": ${zero.toJSON()}}"

        val expected = "\"${zero.compareTo(zero)}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }
}
