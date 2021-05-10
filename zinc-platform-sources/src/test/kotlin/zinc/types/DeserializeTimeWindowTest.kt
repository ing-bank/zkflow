package zinc.types

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.TimeWindow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeTimeWindowTest {
    private val zincZKService = getZincZKService<DeserializeTimeWindowTest>()

    private val timeData: List<TimeWindow> = listOf(
        TimeWindow.fromOnly(Instant.now()),
        TimeWindow.untilOnly(Instant.now()),
        TimeWindow.between(Instant.now(), Instant.now().plusSeconds(3600)),
        TimeWindow.fromStartAndDuration(Instant.now(), Duration.ofSeconds(3600)),
    )

    @ParameterizedTest
    @MethodSource("testData")
    fun `a TimeWindow should be deserialized correctly`(data: Data) {
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        zincZKService.run(witness, expected)
    }

    @Serializable
    data class Data(
        val data: @Contextual TimeWindow
    )

    companion object {
        @JvmStatic
        fun testData() = listOf(
            TimeWindow.fromOnly(Instant.now()),
            TimeWindow.untilOnly(Instant.now()),
            TimeWindow.between(Instant.now(), Instant.now().plusSeconds(3600)),
            TimeWindow.fromStartAndDuration(Instant.now(), Duration.ofSeconds(3600)),
        ).map { Data(it) }
    }
}
