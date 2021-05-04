package zinc.types

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.Year
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeZonedDateTimeTest {
    private val zincZKService = getZincZKService<DeserializeZonedDateTimeTest>()

    @Test
    fun `an Instance should be deserialized correctly`() {
        listOf(
            Data(ZonedDateTime.now()),
            Data(ZonedDateTime.now(ZoneId.of("America/Argentina/ComodRivadavia"))),
            Data(ZonedDateTime.now(ZoneOffset.ofTotalSeconds(42))),
            Data(ZonedDateTime.of(Year.MAX_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"))),
            Data(ZonedDateTime.of(Year.MIN_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT")))
        ).forEach(this::performDeserializationTest)
    }

    private fun performDeserializationTest(data: Data) {
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
