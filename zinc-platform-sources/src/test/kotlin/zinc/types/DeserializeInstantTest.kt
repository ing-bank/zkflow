package zinc.types

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeInstantTest {
    private val zincZKService = getZincZKService<DeserializeInstantTest>()

    @Test
    fun `an Instance should be deserialized correctly`() {
        val data = Data(Instant.ofEpochSecond(42, 79))
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        val actual = zincZKService.run(witness, "")

        val expectedJson = Json.parseToJsonElement(expected)
        val actualJson = Json.parseToJsonElement(actual)

        actualJson shouldBe expectedJson
    }

    @Serializable
    private data class Data(
        val data: @Contextual Instant
    )
}