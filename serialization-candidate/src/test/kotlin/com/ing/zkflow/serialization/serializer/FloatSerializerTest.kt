package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer.FloatSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FloatSerializerTest : SerializerTest {
    private val float = 12.0.toFloat()

    @ParameterizedTest
    @MethodSource("engines")
    fun `Double must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(FloatSerializer, float)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Doubles must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(FloatSerializer, float).size shouldBe
            engine.serialize(FloatSerializer, float + 12.0.toFloat()).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with Double must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsFloat.serializer(), ContainsFloat())
    }

    @Serializable
    data class ContainsFloat(
        @Serializable(with = FloatSerializer::class)
        val double: Float = 10.01.toFloat()
    )
}
