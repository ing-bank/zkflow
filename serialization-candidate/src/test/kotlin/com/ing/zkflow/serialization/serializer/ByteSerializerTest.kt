package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.engine.BFLEngine
import com.ing.zkflow.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ByteSerializerTest : SerializerTest {
    private val byte = 16.toByte()
    private val byteAsByte = byteArrayOf(16)
    private val byteAsBits = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0)

    @ParameterizedTest
    @MethodSource("engines")
    fun `Byte must be serializable`(engine: SerdeEngine) {
        when (engine) {
            is BFLEngine.Bytes -> {
                engine.serialize(ByteSerializer, byte) shouldBe byteAsByte
                engine.deserialize(ByteSerializer, byteAsByte) shouldBe byte
            }
            is BFLEngine.Bits -> {
                engine.serialize(ByteSerializer, byte) shouldBe byteAsBits
                engine.deserialize(ByteSerializer, byteAsBits) shouldBe byte
            }
        }
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
        object Bytes_2 : WrappedKSerializerWithDefault<Byte>(ByteSerializer)
    }
}
