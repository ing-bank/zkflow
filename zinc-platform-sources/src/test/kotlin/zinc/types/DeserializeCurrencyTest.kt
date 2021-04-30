package zinc.types

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeCurrencyTest {
    private val zincZKService = getZincZKService<DeserializeCurrencyTest>()

    @Test
    fun `an Currency should be deserialized correctly`() {
        val data = Data(Currency.getInstance(Locale.FRANCE))
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        val actual = zincZKService.run(witness, "")

        val expectedJson = Json.parseToJsonElement(expected)
        val actualJson = Json.parseToJsonElement(actual)

        actualJson shouldBe expectedJson
    }

    @Serializable
    private data class Data(
        val data: @Contextual Currency
    )
}
