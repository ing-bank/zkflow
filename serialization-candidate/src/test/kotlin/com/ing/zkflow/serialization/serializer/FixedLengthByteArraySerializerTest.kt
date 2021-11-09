package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FixedLengthByteArraySerializerTest : SerializerTest {
    private val byteArray = listOf<Byte>(1, 2, 3).toByteArray()

    @ParameterizedTest
    @MethodSource("engines")
    fun `ByteArray must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, byteArray) { a, b -> a.contentEquals(b) }
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ByteArray-s must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, byteArray).size shouldBe
            engine.serialize(InstanceSerializer, byteArray + 10).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with ByteArray must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsByteArray.serializer(), ContainsByteArray(byteArray)) { a, b -> a.innerByteArray.contentEquals(b.innerByteArray) }
    }

    @Suppress("ArrayInDataClass")
    @Serializable
    data class ContainsByteArray(
        @Serializable(with = InstanceSerializer::class)
        val innerByteArray: ByteArray
    )

    object InstanceSerializer : FixedLengthByteArraySerializer(10)
}
