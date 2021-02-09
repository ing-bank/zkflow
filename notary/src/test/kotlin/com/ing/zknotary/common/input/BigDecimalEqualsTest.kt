package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
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
        val left = BigDecimal("421.42")
        val right = BigDecimal("-421.42")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares using equals (integer is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("42.42")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares using equals (fraction is different)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.421")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc compares using equals (both are equal to each other)`() {
        val left = BigDecimal("421.42")
        val right = BigDecimal("421.42")

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }
}
