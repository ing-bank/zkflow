package com.ing.zkflow.serialization.serializer

import engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ShortSerializerTest : SerializerTest {
    private val short = 1.toShort()

    @ParameterizedTest
    @MethodSource("engines")
    fun `Short must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ShortSerializer, short)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Shorts must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(ShortSerializer, short).size shouldBe
            engine.serialize(ShortSerializer, (short + 2).toShort()).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List Short must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsShorts.serializer(), ContainsShorts())
    }

    @Serializable
    data class ContainsShorts(
        @Serializable(with = ShortSerializer::class)
        val short: Short = 1.toShort(),

        @Serializable(with = Short_0::class)
        val ints: List<Short?> = listOf(1, null)
    ) {
        object Short_0 : FixedLengthListSerializer<Short?>(5, Short_1)
        object Short_1 : NullableSerializer<Short>(Short_2)
        object Short_2 : WrappedKSerializerWithDefault<Short>(ShortSerializer)
    }
}
