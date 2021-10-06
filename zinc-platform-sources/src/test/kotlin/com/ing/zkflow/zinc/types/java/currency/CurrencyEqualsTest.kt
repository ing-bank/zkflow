package com.ing.zkflow.zinc.types.java.currency

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale
import kotlin.time.ExperimentalTime

@ExperimentalTime
class CurrencyEqualsTest {
    private val zincZKService = getZincZKService<CurrencyEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(Currency.getInstance(Locale.FRANCE), Currency.getInstance(Locale.FRANCE), true)
    }

    @Test
    fun `euros should not equal pounds`() {
        performEqualityTest(Currency.getInstance(Locale.FRANCE), Currency.getInstance(Locale.UK), false)
    }

    @Test
    fun `euros should equal euros`() {
        performEqualityTest(Currency.getInstance(Locale.FRANCE), Currency.getInstance(Locale.GERMANY), true)
    }

    private fun performEqualityTest(
        left: Currency,
        right: Currency,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
