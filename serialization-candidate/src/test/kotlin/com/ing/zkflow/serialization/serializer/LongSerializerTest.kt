package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class LongSerializerTest : SerializerTest {
    private val long = 1.toLong()

    @ParameterizedTest
    @MethodSource("engines")
    fun `Long must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(LongSerializer, Long.MIN_VALUE)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Longs must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(LongSerializer, long).size shouldBe
            engine.serialize(LongSerializer, long + 2.toLong()).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List Long must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsLongs.serializer(), ContainsLongs())
    }

    @Serializable
    data class ContainsLongs(
        @Serializable(with = LongSerializer::class)
        val long: Long = 1.toLong(),

        @Serializable(with = Long_0::class)
        val ints: List<Long?> = listOf(1, null)
    ) {
        object Long_0 : FixedLengthListSerializer<Long?>(5, Long_1)
        object Long_1 : NullableSerializer<Long>(Long_2)
        object Long_2 : WrappedKSerializerWithDefault<Long>(LongSerializer)
    }
}
