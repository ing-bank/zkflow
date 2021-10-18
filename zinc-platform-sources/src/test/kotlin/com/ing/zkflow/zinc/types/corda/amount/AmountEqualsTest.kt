package com.ing.zkflow.zinc.types.corda.amount

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.proveTimed
import com.ing.zkflow.zinc.types.setupTimed
import com.ing.zkflow.zinc.types.toWitness
import com.ing.zkflow.zinc.types.verifyTimed
import net.corda.core.contracts.Amount
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

class AmountEqualsTest {
    private val log = loggerFor<AmountEqualsTest>()
    private val zincZKService =
        getZincZKService<AmountEqualsTest>()
    private val dummyToken = Currency.getInstance(Locale.UK)
    private val anotherDummyToken = Currency.getInstance(Locale.FRANCE)

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc equals with different quantities`() {
        val left = Amount(200, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("1"), dummyToken)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc equals with different display token sizes`() {
        val left = Amount(200, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("2"), dummyToken)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc equals with different token hashes`() {
        val left = Amount(200, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("1"), anotherDummyToken)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    @Test
    fun `zinc smoke test`() {
        val left = Amount(100, BigDecimal("1"), dummyToken)
        val right = Amount(100, BigDecimal("1"), dummyToken)

        val input = toWitness(left, right)
        val expected = "\"${if (left == right) 0 else 1}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }
}
