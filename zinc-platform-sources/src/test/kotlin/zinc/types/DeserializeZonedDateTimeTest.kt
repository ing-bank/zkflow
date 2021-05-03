package zinc.types

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeZonedDateTimeTest {
    private val zincZKService = getZincZKService<DeserializeZonedDateTimeTest>()

    @Test
    fun `an Instance should be deserialized correctly`() {
        val data = Data(ZonedDateTime.now())
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        val actual = zincZKService.run(witness, "")

        val expectedJson = Json.parseToJsonElement(expected)
        val actualJson = Json.parseToJsonElement(actual)

        actualJson shouldBe expectedJson
    }

    @Serializable
    private data class Data(
        val data: @Contextual ZonedDateTime
    )
}
