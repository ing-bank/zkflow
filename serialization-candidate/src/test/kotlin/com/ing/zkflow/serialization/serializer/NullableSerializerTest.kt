package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class NullableSerializerTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `Nullable must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, null)
        engine.assertRoundTrip(InstanceSerializer, 100)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Nullables must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, null).size shouldBe
            engine.serialize(InstanceSerializer, 100).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with nullable fields must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsNullable.serializer(), ContainsNullable())
    }

    @Serializable
    data class ContainsNullable(
        @Serializable(with = InstanceSerializer::class)
        val intNullValue: Int? = null,
        @Serializable(with = InstanceSerializer::class)
        val intNonNullValue: Int? = 100
    )

    object InstanceSerializer : NullableSerializer<Int>(IntSerializer)
}
