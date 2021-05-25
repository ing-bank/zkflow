package com.ing.zknotary.zinc.types.zoneddatetime

import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toWitness
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Year
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeZonedDateTimeTest {
    private val zincZKService = getZincZKService<DeserializeZonedDateTimeTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun performDeserializationTest(data: Data) {
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        zincZKService.run(witness, expected)
    }

    @Serializable
    data class Data(
        val data: @Contextual ZonedDateTime
    )

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(ZonedDateTime.now()),
            Data(ZonedDateTime.now(ZoneId.of("America/Argentina/ComodRivadavia"))),
            Data(ZonedDateTime.now(ZoneOffset.ofTotalSeconds(42))),
            Data(ZonedDateTime.of(Year.MAX_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"))),
            Data(ZonedDateTime.of(Year.MIN_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT")))
        )
    }
}
