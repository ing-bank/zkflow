package com.ing.zkflow.serialization.serializer

import engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class DoubleSerializerTest : SerializerTest {
    private val double = 12.0

    @ParameterizedTest
    @MethodSource("engines")
    fun `Double must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(DoubleSerializer, double)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Doubles must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(DoubleSerializer, double).size shouldBe
            engine.serialize(DoubleSerializer, double + 12.0).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with Double must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsDouble.serializer(), ContainsDouble())
    }

    @Serializable
    data class ContainsDouble(
        @Serializable(with = DoubleSerializer::class)
        val double: Double = 10.01
    )
}
