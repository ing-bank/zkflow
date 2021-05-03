package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.zone.ZoneRulesProvider

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> by (
    SurrogateSerializer(ZonedDateTimeSurrogate.serializer()) {
        ZonedDateTimeSurrogate.from(
            it
        )
    }
    )

/**
 * BFL surrogate for [ZonedDateTime].
 * In order to save space in the serialized format, we serialize a hash of the ZoneRegion
 * and lookup the original in a map of known ZoneIds. The hashing format is described in the
 * documentation of the [String.hashCode()] method and is supposed to be stable across
 * different versions and implementations of the jvm.
 * @property year the year to represent, from MIN_YEAR to MAX_YEAR (999_999_999)
 * @property month the month-of-year to represent, from 1 (January) to 12 (December)
 * @property dayOfMonth the day-of-month to represent, from 1 to 31
 * @property hour the hour-of-day to represent, from 0 to 23
 * @property minute the minute-of-hour to represent, from 0 to 59
 * @property second the second-of-minute to represent, from 0 to 59
 * @property nanoOfSecond the nano-of-second to represent, from 0 to 999,999,999
 * @property zoneOffsetSeconds the zone offset in seconds
 * @property zoneIdHash the hash of the string representation of ZoneId, or 0 if unknown TODO explain hashing & lookup
 */
@Serializable
data class ZonedDateTimeSurrogate(
    val year: Int,
    val month: Byte,
    val dayOfMonth: Byte,
    val hour: Byte,
    val minute: Byte,
    val second: Byte,
    val nanoOfSecond: Int,
    val zoneOffsetSeconds: Int,
    val zoneIdHash: Int,
) : Surrogate<ZonedDateTime> {

    override fun toOriginal(): ZonedDateTime {
        val zoneId = when (zoneIdHash) {
            0 -> ZoneOffset.ofTotalSeconds(zoneOffsetSeconds)
            else -> zoneRegionMap[zoneIdHash]?.let { ZoneId.of(it) }
                ?: ZoneOffset.ofTotalSeconds(zoneOffsetSeconds)
        }
        return ZonedDateTime.of(
            year,
            month.toInt(),
            dayOfMonth.toInt(),
            hour.toInt(),
            minute.toInt(),
            second.toInt(),
            nanoOfSecond,
            zoneId
        )
    }

    companion object {
        private val zoneRegionMap: Map<Int, String> by lazy {
            ZoneRulesProvider.getAvailableZoneIds().associateBy {
                it.hashCode()
            }
        }

        fun from(original: ZonedDateTime) = with(original) {
            when (zone) {
                is ZoneOffset -> {
                    val offsetZone = zone as ZoneOffset
                    ZonedDateTimeSurrogate(
                        year,
                        month.value.toByte(),
                        dayOfMonth.toByte(),
                        hour.toByte(),
                        minute.toByte(),
                        second.toByte(),
                        nano,
                        offsetZone.totalSeconds,
                        0
                    )
                }
                else -> {
                    ZonedDateTimeSurrogate(
                        year,
                        month.value.toByte(),
                        dayOfMonth.toByte(),
                        hour.toByte(),
                        minute.toByte(),
                        second.toByte(),
                        nano,
                        offset.totalSeconds,
                        zone.id.hashCode()
                    )
                }
            }
        }
    }
}
