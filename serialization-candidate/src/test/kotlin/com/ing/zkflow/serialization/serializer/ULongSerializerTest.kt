package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ULongSerializerTest : SerializerTest {
    private val long = 1.toULong()

    @ParameterizedTest
    @MethodSource("engines")
    fun `ULong must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ULongSerializer, ULong.MIN_VALUE)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ULongs must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(ULongSerializer, long).size shouldBe
            engine.serialize(ULongSerializer, long + 2.toULong()).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List ULong must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsULongs.serializer(), ContainsULongs())
    }

    @Serializable
    data class ContainsULongs(
        @Serializable(with = ULongSerializer::class)
        val long: ULong = 1.toULong(),

        @Serializable(with = ULong_0::class)
        val ints: List<ULong?> = listOf(1U, null)
    ) {
        object ULong_0 : FixedLengthListSerializer<ULong?>(5, ULong_1)
        object ULong_1 : NullableSerializer<ULong>(ULong_2)
        object ULong_2 : WrappedKSerializerWithDefault<ULong>(ULongSerializer)
    }
}
