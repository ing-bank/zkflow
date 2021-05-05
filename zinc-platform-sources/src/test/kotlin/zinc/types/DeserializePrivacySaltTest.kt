package zinc.types

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.corda.core.contracts.PrivacySalt
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializePrivacySaltTest {
    private val zincZKService = getZincZKService<DeserializePrivacySaltTest>()

    private val privacySalt = PrivacySalt(32)

    @Test
    fun `a Privacy should be deserialized correctly`() {
        val data = Data(privacySalt)
        val witness = toWitness(data)
        val expected = data.data.toZincJson()
        val actual = zincZKService.run(witness, "")

        val expectedJson = Json.parseToJsonElement(expected)
        val actualJson = Json.parseToJsonElement(actual)

        actualJson shouldBe expectedJson
    }

    @Serializable
    private data class Data(
        val data: @Contextual PrivacySalt
    )
}
