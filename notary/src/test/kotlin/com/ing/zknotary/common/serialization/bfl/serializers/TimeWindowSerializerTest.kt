package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.zknotary.testing.roundTrip
import com.ing.zknotary.testing.sameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.TimeWindow
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class TimeWindowSerializerTest {
    @Serializable
    data class Data(val value: @Contextual TimeWindow)

    private val testList: List<TimeWindow> = listOf(
        TimeWindow.fromOnly(Instant.now()),
        TimeWindow.untilOnly(Instant.now()),
        TimeWindow.between(Instant.now(), Instant.now().plusSeconds(3600)),
        TimeWindow.fromStartAndDuration(Instant.now(), Duration.ofSeconds(3600)),
    )

    @Test
    fun `TimeWindow serializer`() {
        testList.forEach {
            roundTrip(it)
        }

        sameSize(testList[0], testList[1])
    }

    @Test
    fun `TimeWindow as part of structure serializer`() {
        testList
            .map { Data(it) }
            .forEach { roundTrip(it) }

        sameSize(Data(testList[0]), Data(testList[1]))
    }
}
