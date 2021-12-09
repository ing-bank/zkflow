package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UShortSerializerTest : SerializerTest {
    private val short = 1.toUShort()

    @ParameterizedTest
    @MethodSource("engines")
    fun `UShort must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(UShortSerializer, short)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `UShorts must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(UShortSerializer, short).size shouldBe
            engine.serialize(UShortSerializer, (short + 2U).toUShort()).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List UShort must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsUShorts.serializer(), ContainsUShorts())
    }

    @Serializable
    data class ContainsUShorts(
        @Serializable(with = UShortSerializer::class)
        val short: UShort = 1.toUShort(),

        @Serializable(with = UShort_0::class)
        val ints: List<UShort?> = listOf(1U, null)
    ) {
        object UShort_0 : FixedLengthListSerializer<UShort?>(5, UShort_1)
        object UShort_1 : NullableSerializer<UShort>(UShort_2)
        object UShort_2 : WrappedKSerializerWithDefault<UShort>(UShortSerializer)
    }
}
