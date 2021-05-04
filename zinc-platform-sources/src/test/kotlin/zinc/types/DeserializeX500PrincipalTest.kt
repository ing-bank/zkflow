package zinc.types

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import javax.security.auth.x500.X500Principal
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeX500PrincipalTest {
    private val zincZKService = getZincZKService<DeserializeX500PrincipalTest>()

    @Test
    fun `an X500Principal should be deserialized correctly`() {
        val data = Data(X500Principal("CN=Steve Kille,O=Isode Limited,C=GB"))
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        val actual = zincZKService.run(witness, "")

        val expectedJson = Json.parseToJsonElement(expected)
        val actualJson = Json.parseToJsonElement(actual)

        actualJson shouldBe expectedJson
    }

    @Serializable
    private data class Data(
        val data: @Contextual X500Principal
    )
}