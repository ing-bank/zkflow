package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration

class AmountEqualsTest {
    companion object {
        private val circuitFolder: String = AmountEqualsTest::class.java.getResource("/AmountEqualsTest").path
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

        const val DUMMY_TOKEN = "com.ing.zknotary.SuperToken"
        const val ANOTHER_DUMMY_TOKEN = 420
    }

    @Test
    fun `zinc equals with different quantities`() {
        val left = Amount(200, BigDecimal("1"), DUMMY_TOKEN)
        val right = Amount(100, BigDecimal("1"), DUMMY_TOKEN)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc equals with different display token sizes`() {
        val left = Amount(200, BigDecimal("1"), DUMMY_TOKEN)
        val right = Amount(100, BigDecimal("2"), DUMMY_TOKEN)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc equals with different token hashes`() {
        val left = Amount(200, BigDecimal("1"), DUMMY_TOKEN)
        val right = Amount(100, BigDecimal("1"), ANOTHER_DUMMY_TOKEN)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }

    @Test
    fun `zinc smoke test`() {
        val left = Amount(100, BigDecimal("1"), DUMMY_TOKEN)
        val right = Amount(100, BigDecimal("1"), DUMMY_TOKEN)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = "\"${if (left == right) 0 else 1}\""

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toByteArray())
    }
}
