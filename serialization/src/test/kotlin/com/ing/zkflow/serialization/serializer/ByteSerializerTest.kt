package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ByteSerializerTest : SerializerTest {
    private val byte = 16.toByte()

    @ParameterizedTest
    @MethodSource("engines")
    fun `Int must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ByteSerializer, byte)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Bytes must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(ByteSerializer, byte).size shouldBe
            engine.serialize(ByteSerializer, 25.toByte()).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List Byte must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsBytes.serializer(), ContainsBytes())
    }

    @Serializable
    data class ContainsBytes(
        @Serializable(with = ByteSerializer::class)
        val byte: Byte = 0,

        @Serializable(with = Bytes_0::class)
        val bytes: List<Byte?> = listOf(45, null)
    ) {
        object Bytes_0 : FixedLengthListSerializer<Byte?>(5, Bytes_1)
        object Bytes_1 : NullableSerializer<Byte>(Bytes_2)
        object Bytes_2 : WrappedFixedLengthKSerializerWithDefault<Byte>(ByteSerializer)
    }
}
