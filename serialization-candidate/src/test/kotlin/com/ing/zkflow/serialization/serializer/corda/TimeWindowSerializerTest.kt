package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.TimeWindow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Instant

class TimeWindowSerializerTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `Timewindows must serialize and deserialize`(engine: SerdeEngine) {
        timeWindows.forEach {
            engine.assertRoundTrip(TimeWindowSerializer, it)
        }
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Timewindow serializations must have constant size`(engine: SerdeEngine) {
        engine.serialize(TimeWindowSerializer, timeWindows[0]).size shouldBe
            engine.serialize(TimeWindowSerializer, timeWindows[1]).size
    }

    private val timeWindows: List<TimeWindow> = listOf(
        TimeWindow.fromOnly(Instant.now()),
        TimeWindow.untilOnly(Instant.now()),
        TimeWindow.between(Instant.now(), Instant.now().plusSeconds(3600)),
        TimeWindow.fromStartAndDuration(Instant.now(), Duration.ofSeconds(3600)),
    )
}
