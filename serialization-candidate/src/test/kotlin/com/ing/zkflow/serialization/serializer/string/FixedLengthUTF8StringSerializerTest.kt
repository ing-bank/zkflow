package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.SerializerTest
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FixedLengthUTF8StringSerializerTest : SerializerTest {
    private val string = "a的"

    @ParameterizedTest
    @MethodSource("engines")
    fun `UTF8 string must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, string)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `UTF strings must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, string).size shouldBe
            engine.serialize(InstanceSerializer, string + "b").size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with UTF string must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsString.serializer(), ContainsString(string))
    }

    @Serializable
    data class ContainsString(
        @Serializable(with = InstanceSerializer::class)
        val innerString: String
    )

    object InstanceSerializer : FixedLengthUTF8StringSerializer(5)
}
