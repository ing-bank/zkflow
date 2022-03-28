package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FixedLengthStringSerializerTest : SerializerTest {
    companion object {
        private const val asciiString = "az"
        private const val utf8String = "a的"
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ASCII string must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, asciiString)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ASCII strings must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, asciiString).size shouldBe
            engine.serialize(InstanceSerializer, asciiString + "b").size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with ASCII string must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsString.serializer(), ContainsString(asciiString))
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `UTF8 string must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, utf8String)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `UTF strings must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, utf8String).size shouldBe
            engine.serialize(InstanceSerializer, utf8String + "b").size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with UTF string must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsString.serializer(), ContainsString(utf8String))
    }

    @Serializable
    data class ContainsString(
        @Serializable(with = InstanceSerializer::class)
        val innerString: String
    )

    object InstanceSerializer : FixedLengthStringSerializer(5)
}
