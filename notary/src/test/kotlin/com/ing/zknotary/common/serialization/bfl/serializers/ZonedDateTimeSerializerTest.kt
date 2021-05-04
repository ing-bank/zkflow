package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.time.Year
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import com.ing.serialization.bfl.api.reified.debugSerialize as debugSerializeInlined

class ZonedDateTimeSerializerTest {
    @Serializable
    data class Data(val date: @Contextual ZonedDateTime)

    @Test
    fun `ZonedDateTime should be serialized successfully`() {
        val data = Data(ZonedDateTime.now())
        println(debugSerializeInlined(data, serializersModule = CordaSerializers).second)
    }

    @Test
    fun `ZonedDateTime should be the same after serialization and deserialization`() {
        listOf(
            Data(ZonedDateTime.now()),
            Data(ZonedDateTime.now(ZoneId.of("America/Argentina/ComodRivadavia"))),
            Data(ZonedDateTime.now(ZoneOffset.ofTotalSeconds(42))),
            Data(ZonedDateTime.of(Year.MAX_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"))),
            Data(ZonedDateTime.of(Year.MIN_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"))),
        ).forEach { data ->
            assertRoundTripSucceeds(data)
        }
    }

    @Test
    fun `different ZonedDateTimes should have same size after serialization`() {
        val data1 = Data(ZonedDateTime.now(ZoneId.of("America/Argentina/ComodRivadavia")))
        val data2 = Data(ZonedDateTime.now(ZoneOffset.ofTotalSeconds(42)))

        assertSameSize(data1, data2)
    }
}
