package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.serializer.SerializerTest
import engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FixedLengthASCIIStringSerializerTest : SerializerTest {
    private val string = "az"

    @ParameterizedTest
    @MethodSource("engines")
    fun `ASCII string must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, string)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ASCII strings must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, string).size shouldBe
            engine.serialize(InstanceSerializer, string + "b").size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with ASCII string must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsString.serializer(), ContainsString(string))
    }

    @Serializable
    data class ContainsString(
        @Serializable(with = InstanceSerializer::class)
        val innerString: String
    )

    object InstanceSerializer : FixedLengthASCIIStringSerializer(3)
}
