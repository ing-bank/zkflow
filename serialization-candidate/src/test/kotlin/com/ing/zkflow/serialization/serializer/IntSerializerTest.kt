package com.ing.zkflow.serialization.serializer

import engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class IntSerializerTest : SerializerTest {
    private val int = 1

    @ParameterizedTest
    @MethodSource("engines")
    fun `Int must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(IntSerializer, int)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Ints must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(IntSerializer, int).size shouldBe
            engine.serialize(IntSerializer, int + 1).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List Int must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsInts.serializer(), ContainsInts())
    }

    @Serializable
    data class ContainsInts(
        @Serializable(with = IntSerializer::class)
        val int: Int = 1,

        @Serializable(with = Int_0::class)
        val ints: List<Int?> = listOf(1, null)
    ) {
        object Int_0 : FixedLengthListSerializer<Int?>(5, Int_1)
        object Int_1 : NullableSerializer<Int>(Int_2)
        object Int_2 : WrappedKSerializerWithDefault<Int>(IntSerializer)
    }
}
