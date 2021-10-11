package com.ing.zkflow.zinc.types.java.currency

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toWitness
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeCurrencyTest {
    private val zincZKService = getZincZKService<DeserializeCurrencyTest>()

    @Test
    fun `a Currency should be deserialized correctly`() {
        val data = Data(Currency.getInstance(Locale.FRANCE))
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        zincZKService.run(witness, expected)
    }

    @Serializable
    private data class Data(
        val data: @Contextual Currency
    )
}
