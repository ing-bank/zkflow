package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UByteSerializerTest : SerializerTest {
    private val byte = 16.toUByte()

    @ParameterizedTest
    @MethodSource("engines")
    fun `Int must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(UByteSerializer, byte)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `UBytes must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(UByteSerializer, byte).size shouldBe
            engine.serialize(UByteSerializer, 25.toUByte()).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List UByte must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsUBytes.serializer(), ContainsUBytes())
    }

    @Serializable
    data class ContainsUBytes(
        @Serializable(with = UByteSerializer::class)
        val byte: UByte = 0U,

        @Serializable(with = UBytes_0::class)
        val bytes: List<UByte?> = listOf(45U, null)
    ) {
        object UBytes_0 : FixedLengthListSerializer<UByte?>(5, UBytes_1)
        object UBytes_1 : NullableSerializer<UByte>(UBytes_2)
        object UBytes_2 : WrappedKSerializerWithDefault<UByte>(UByteSerializer)
    }
}
