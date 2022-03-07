package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.Surrogate
import com.ing.zkflow.serialization.serializer.InstantSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.TimeWindow
import java.time.Instant

object TimeWindowSerializer :
    SurrogateSerializer<TimeWindow, TimeWindowSurrogate>(
        TimeWindowSurrogate.serializer(),
        { TimeWindowSurrogate.from(it) }
    )

@Serializable
data class TimeWindowSurrogate(
    @Serializable(with = TimeBoundSerializer::class) val fromTime: Instant?,
    @Serializable(with = TimeBoundSerializer::class) val untilTime: Instant?
) : Surrogate<TimeWindow> {

    object TimeBoundSerializer : NullableSerializer<Instant>(InstantSerializer)

    override fun toOriginal(): TimeWindow {
        return when {
            fromTime != null && untilTime != null -> TimeWindow.between(fromTime, untilTime)
            fromTime != null -> TimeWindow.fromOnly(fromTime)
            untilTime != null -> TimeWindow.untilOnly(untilTime)
            else -> error("Serialization of `${TimeWindow::class.qualifiedName}` is corrupted")
        }
    }

    companion object {
        fun from(original: TimeWindow): TimeWindowSurrogate = TimeWindowSurrogate(original.fromTime, original.untilTime)
    }
}
