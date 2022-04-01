package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FixedSizeUtf16StringSerializerTest : SerializerTest {
    companion object {
        private const val asciiString = "az"
        private const val unicodeString = "a的"
        private const val tooLongString = "This string is way too long."
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `too long string should fail`(engine: SerdeEngine) {
        shouldThrow<IllegalArgumentException> {
            engine.assertRoundTrip(InstanceSerializer, tooLongString)
        }.message shouldBe "UTF-16 encoding of String `This string is way too long.` (58) is longer than 8."
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
    fun `unicode string must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, unicodeString)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `UTF strings must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, unicodeString).size shouldBe
            engine.serialize(InstanceSerializer, unicodeString + "b").size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with UTF string must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsString.serializer(), ContainsString(unicodeString))
    }

    @Serializable
    data class ContainsString(
        @Serializable(with = InstanceSerializer::class)
        val innerString: String
    )

    object InstanceSerializer : FixedSizeUtf16StringSerializer(8)
}
