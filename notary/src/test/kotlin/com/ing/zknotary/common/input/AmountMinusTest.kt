package com.ing.zknotary.common.input

import com.ing.zknotary.common.zkp.ZKProvingException
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration

class AmountMinusTest {
    companion object {
        private val circuitFolder: String = AmountMinusTest::class.java.getResource("/AmountMinusTest").path
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
    fun `zinc plus fails due to different token sizes`() {
        val exception = Assertions.assertThrows(ZKProvingException::class.java) {
            val left = Amount(200, BigDecimal("10"), DUMMY_TOKEN)
            val right = Amount(100, BigDecimal("1"), DUMMY_TOKEN)

            val input = "{\"left\": ${left.toJSON()}" +
                ",\"right\": ${right.toJSON()}}"

            zincZKService.prove(input.toByteArray())
        }

        Assertions.assertTrue(
            exception.message?.contains("Token sizes don't match") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc plus fails due to different token hashes`() {
        val exception = Assertions.assertThrows(ZKProvingException::class.java) {
            val left = Amount(1, BigDecimal("1"), DUMMY_TOKEN)
            val right = Amount(1, BigDecimal("1"), ANOTHER_DUMMY_TOKEN)

            val input = "{\"left\": ${left.toJSON()}" +
                ",\"right\": ${right.toJSON()}}"

            zincZKService.prove(input.toByteArray())
        }

        Assertions.assertTrue(
            exception.message?.contains("Tokens don't match") ?: false,
            "Circuit fails with different error"
        )
    }

    @Test
    fun `zinc minus smoke test`() {
        val left = Amount(200, BigDecimal("1"), DUMMY_TOKEN)
        val right = Amount(100, BigDecimal("1"), DUMMY_TOKEN)

        val input = "{\"left\": ${left.toJSON()}" +
            ",\"right\": ${right.toJSON()}}"

        val expected = left.minus(right)

        val proof = zincZKService.prove(input.toByteArray())
        zincZKService.verify(proof, expected.toJSON().toByteArray())
    }
}
