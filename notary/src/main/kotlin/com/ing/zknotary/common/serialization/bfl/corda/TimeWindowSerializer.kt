package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.TimeWindow
import java.time.Instant

object TimeWindowSerializer : KSerializer<TimeWindow> by (
        SurrogateSerializer(TimeWindowSurrogate.serializer()) { TimeWindowSurrogate.from(it) }
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
            else -> error("Time window with undefined limits. Please provide at least an upper or lower limit")
        }
    }

    companion object {
        fun from(original: TimeWindow): TimeWindowSurrogate = TimeWindowSurrogate(original.fromTime, original.untilTime)
    }
}
