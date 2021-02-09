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
}
