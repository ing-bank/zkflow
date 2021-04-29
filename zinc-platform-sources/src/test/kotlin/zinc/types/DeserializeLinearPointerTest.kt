package zinc.types

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeLinearPointerTest {
    private val zincZKService = getZincZKService<DeserializeLinearPointerTest>()

    @Test
    fun `a LinearPointer should be deserialized correctly`() {
        val data = Data(LinearPointer(getSomeId(), MyLinearState::class.java, true))
        val witness = toWitness(data)

        val expected = data.pointer.toZincJson()
        val actual = zincZKService.run(witness, "")

        val expectedJson = Json.parseToJsonElement(expected)
        val actualJson = Json.parseToJsonElement(actual)

        actualJson shouldBe expectedJson
    }

    @Serializable
    private data class MyLinearState(
        override val participants: List<@Contextual AbstractParty>,
        override val linearId: @Contextual UniqueIdentifier
    ) : LinearState

    @Serializable
    private data class Data(
        val pointer: @Contextual LinearPointer<MyLinearState>
    )

    private fun getSomeId() = UniqueIdentifier(
        externalId = "some.id",
        id = UUID(0L, 42L)
    )
}
