package com.ing.zkflow.serialization.scheme

import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer.DoubleSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer.FloatSerializer
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.char.UnicodeCharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUtf8StringSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.DataOutput

@Suppress("TooManyFunctions")
internal sealed class BinaryEncoder(
    internal val output: DataOutput,
) : AbstractEncoder() {
    class BitBinaryEncoder(output: DataOutput) : BinaryEncoder(output) {
        companion object {
            const val BINARY_STRING_PAD_CHAR = '0'
            const val BOOLEAN_PADDING = 7
        }

        @Suppress("MagicNumber")
        override fun encodeByte(value: Byte) = Integer
            .toBinaryString(value.toInt() and 0xFF)
            .padStart(Byte.SIZE_BITS, BINARY_STRING_PAD_CHAR)
            .toCharArray()
            .map { "$it".toByte() }
            .toByteArray()
            .let { output.write(it) }

        @Suppress("MagicNumber")
        override fun encodeShort(value: Short) =
            Integer
                .toBinaryString(value.toInt() and 0xFFFF)
                .padStart(Short.SIZE_BITS, BINARY_STRING_PAD_CHAR)
                .toCharArray()
                .map { "$it".toByte() }
                .toByteArray()
                .let { output.write(it) }

        override fun encodeInt(value: Int) =
            Integer
                .toBinaryString(value)
                .padStart(Int.SIZE_BITS, BINARY_STRING_PAD_CHAR)
                .toCharArray()
                .map { "$it".toByte() }
                .toByteArray()
                .let { output.write(it) }

        override fun encodeLong(value: Long) =
            java.lang.Long
                .toBinaryString(value)
                .padStart(Long.SIZE_BITS, BINARY_STRING_PAD_CHAR)
                .toCharArray()
                .map { "$it".toByte() }
                .toByteArray()
                .let { output.write(it) }

        override fun encodeBoolean(value: Boolean) = (List(BOOLEAN_PADDING) { false } + value).forEach { output.writeBoolean(it) }
    }

    class ByteBinaryEncoder(output: DataOutput) : BinaryEncoder(output) {
        override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
        override fun encodeShort(value: Short) = output.writeShort(value.toInt())
        override fun encodeInt(value: Int) = output.writeInt(value)
        override fun encodeLong(value: Long) = output.writeLong(value)
        override fun encodeBoolean(value: Boolean) = output.writeBoolean(value)
    }

    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = encodeInt(index)

    override fun encodeFloat(value: Float) {
        WILL_NOT_IMPLEMENT("To encode Float, use ${FloatSerializer::class.qualifiedName}")
    }
    override fun encodeDouble(value: Double) {
        WILL_NOT_IMPLEMENT("To encode Double, use ${DoubleSerializer::class.qualifiedName}")
    }
    override fun encodeChar(value: Char) {
        WILL_NOT_IMPLEMENT("To encode Char, use ${ASCIICharSerializer::class.qualifiedName} or ${UnicodeCharSerializer::class.qualifiedName}")
    }
    override fun encodeString(value: String) {
        WILL_NOT_IMPLEMENT("To encode String, use ${FixedLengthUtf8StringSerializer::class.qualifiedName}")
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        WILL_NOT_IMPLEMENT("To encode Collection, use appropriate fixed-length serializer")
    }

    override fun encodeNull() {
        WILL_NOT_IMPLEMENT("Nulls are not directly encodable")
    }
    override fun encodeNotNullMark() {
        WILL_NOT_IMPLEMENT("Nulls are not directly encodable")
    }
}
