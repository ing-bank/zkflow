package com.ing.zkflow.serialization.serializer.char

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.BFLEngine
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UTF8CharSerializerTest : SerializerTest {
    private val char = '的'

    @ParameterizedTest
    @MethodSource("engines")
    fun `UTF char must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(UTF8CharSerializer, char)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `UTF8 chars must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(UTF8CharSerializer, char).size shouldBe
            engine.serialize(UTF8CharSerializer, 'z').size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List UTF8 char must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsChars.serializer(), ContainsChars())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Sizes for Utf8 chars must match`(engine: BFLEngine) {
        val expectedBytesSize = Short.SIZE_BYTES + // 1 utf 8 char
            Int.SIZE_BYTES + // length of the list
            ContainsChars.MAX_SIZE * (
            Byte.SIZE_BYTES + // nullability flag
                Short.SIZE_BYTES // ut8 char
            )

        ContainsChars.serializer().descriptor.toFixedLengthSerialDescriptorOrThrow().byteSize shouldBe expectedBytesSize
        engine.serialize(ContainsChars.serializer(), ContainsChars()).size shouldBe expectedBytesSize * engine.bytesScaler
    }

    @Serializable
    data class ContainsChars(
        @Serializable(with = UTF8CharSerializer::class)
        val char: Char = '的',

        @Serializable(with = Char_0::class)
        val chars: List<Char?> = listOf('的', null)
    ) {
        object Char_0 : FixedLengthListSerializer<Char?>(MAX_SIZE, Char_1)
        object Char_1 : NullableSerializer<Char>(Char_2)
        object Char_2 : WrappedFixedLengthKSerializerWithDefault<Char>(UTF8CharSerializer)

        companion object {
            const val MAX_SIZE = 5
        }
    }
}
