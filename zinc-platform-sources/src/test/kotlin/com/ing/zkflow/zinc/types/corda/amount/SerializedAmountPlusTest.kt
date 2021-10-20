package com.ing.zkflow.zinc.types.corda.amount

import com.ing.zkflow.common.zkp.ZKProvingException
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.testing.zkp.proveTimed
import com.ing.zkflow.testing.zkp.setupTimed
import com.ing.zkflow.testing.zkp.verifyTimed
import com.ing.zkflow.zinc.types.toSerializedWitness
import com.ing.zkflow.zinc.types.toZincJson
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

class SerializedAmountPlusTest {
    private val zincZKService = getZincZKService<SerializedAmountPlusTest>()
    private val dummyToken = Currency.getInstance(Locale.UK)
    private val anotherDummyToken = Currency.getInstance(Locale.US)

    @BeforeAll
    fun `init`() {
        zincZKService.setupTimed()
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc plus fails due to different token sizes`() {
        val left = Amount(200, BigDecimal("10"), dummyToken)
        val right = Amount(100, BigDecimal("1"), dummyToken)

        val input = toSerializedWitness(left, right)

        assertThrows<ZKProvingException> {
            zincZKService.proveTimed(input)
        }.also {
            assertTrue(
                it.message?.contains("Token sizes don't match") ?: false,
                "Circuit fails with different error"
            )
        }
    }

    @Test
    fun `zinc plus fails due to different token hashes`() {
        val left = Amount(1, BigDecimal("1"), dummyToken)
        val right = Amount(1, BigDecimal("1"), anotherDummyToken)

        val input = toSerializedWitness(left, right)

        assertThrows<ZKProvingException> {
            zincZKService.proveTimed(input)
        }.also {
            assertTrue(
                it.message?.contains("Tokens don't match") ?: false,
                "Circuit fails with different error"
            )
        }
    }

    @Test
    fun `zinc plus fails due to different token types`() {
        val left = Amount(1, BigDecimal("1"), dummyToken)
        val right = Amount(1, BigDecimal("1"), "BR")

        val input = toSerializedWitness(left, right)

        assertThrows<ZKProvingException> {
            zincZKService.proveTimed(input)
        }.also {
            assertTrue(
                it.message?.contains("Token types don't match") ?: false,
                "Circuit fails with different error"
            )
        }
    }

    @Test
    fun `zinc plus smoke test`() {
        val left = Amount(100, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("1"), dummyToken)

        val input = toSerializedWitness(left, right)

        val expected = left.plus(right).toZincJson(
            integerSize = 100,
            fractionSize = 20
        )

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }
}
