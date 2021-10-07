package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
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
    val fromTime: @Contextual Instant?,
    val untilTime: @Contextual Instant?
) : Surrogate<TimeWindow> {

    override fun toOriginal(): TimeWindow {
        return when {
            fromTime != null && untilTime != null -> TimeWindow.between(fromTime, untilTime)
            fromTime != null -> TimeWindow.fromOnly(fromTime)
            untilTime != null -> TimeWindow.untilOnly(untilTime)
            else -> error("Serialization of time window is corrupted")
        }
    }

    companion object {
        fun from(original: TimeWindow): TimeWindowSurrogate = TimeWindowSurrogate(original.fromTime, original.untilTime)
    }
}
