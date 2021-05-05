package zinc.types

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.corda.core.contracts.TimeWindow
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeTimeWindowTest {
    private val zincZKService = getZincZKService<DeserializeTimeWindowTest>()

    private val timeWindows: List<TimeWindow> = listOf(
        TimeWindow.fromOnly(Instant.now()),
        TimeWindow.untilOnly(Instant.now()),
        TimeWindow.between(Instant.now(), Instant.now().plusSeconds(3600)),
        TimeWindow.fromStartAndDuration(Instant.now(), Duration.ofSeconds(3600)),
    )

    @Test
    fun `a TimeWindow should be deserialized correctly`() {
        timeWindows.forEach {
            val data = Data(it)
            val witness = toWitness(data)

            val expected = data.data.toZincJson()
            val actual = zincZKService.run(witness, "")

            val expectedJson = Json.parseToJsonElement(expected)
            val actualJson = Json.parseToJsonElement(actual)

            actualJson shouldBe expectedJson
        }
    }

    @Serializable
    private data class Data(
        val data: @Contextual TimeWindow
    )
}
