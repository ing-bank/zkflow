package com.ing.zkflow.serialization.scheme

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.DataInput
import java.nio.ByteBuffer

@Suppress("TooManyFunctions")
internal sealed class BinaryDecoder(internal val input: DataInput) : AbstractDecoder() {

    class BitBinaryDecoder(input: DataInput) : BinaryDecoder(input) {
        override fun decodeByte(): Byte = input
            .readFully(Byte.SIZE_BITS)
            .joinToString(separator = "")
            .toInt(2)
            .toByte()

        override fun decodeShort(): Short = input
            .readFully(Short.SIZE_BITS)
            .joinToString(separator = "")
            .toInt(2)
            .toShort()

        override fun decodeInt(): Int = input
            .readFully(Int.SIZE_BITS)
            .joinToString(separator = "")
            .toLong(2)
            .toInt()

        // Usually bit conversion is performed by built-in methods via treating the bit string as if it was a larger type,
        // e.g., Byte is parsed as Int and then converted to Byte, otherwise the built-in methods fails for edge cases.
        // In case of Long, there is no larger numeric type, so it is parsed byte-wise.
        override fun decodeLong(): Long = input
            .readFully(Long.SIZE_BITS)
            .toList()
            .chunked(Byte.SIZE_BITS) {
                it.toByteArray().joinToString(separator = "")
                    .toInt(2)
                    .toByte()
            }
            .toByteArray()
            .let {
                ByteBuffer.wrap(it).long
            }

        override fun decodeBoolean(): Boolean {
            input.skipBytes(BinaryEncoder.BitBinaryEncoder.BOOLEAN_PADDING)
            return input.readBoolean()
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            BitBinaryDecoder(input)

        private fun DataInput.readFully(n: Int): ByteArray {
            val buffer = ByteArray(n)
            readFully(buffer)
            return buffer
        }
    }

    class ByteBinaryDecoder(input: DataInput) : BinaryDecoder(input) {
        override fun decodeByte(): Byte = input.readByte()
        override fun decodeShort(): Short = input.readShort()
        override fun decodeInt(): Int = input.readInt()
        override fun decodeLong(): Long = input.readLong()
        override fun decodeBoolean(): Boolean = input.readBoolean()
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            ByteBinaryDecoder(input)
    }

    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeSequentially(): Boolean = true
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = decodeInt()

    override fun decodeFloat(): Float {
        WILL_NOT_IMPLEMENT("Floats are not directly decodable")
    }
    override fun decodeDouble(): Double {
        WILL_NOT_IMPLEMENT("Doubles are not directly decodable")
    }
    override fun decodeChar(): Char {
        WILL_NOT_IMPLEMENT("Chars are not directly decodable")
    }
    override fun decodeString(): String {
        WILL_NOT_IMPLEMENT("Strings are not directly decodable")
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        WILL_NOT_IMPLEMENT("Collection are not directly decodable")
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        WILL_NOT_IMPLEMENT("Collections are not directly decodable")
    }

    override fun decodeNotNullMark(): Boolean {
        WILL_NOT_IMPLEMENT("Nulls are not directly decodable")
    }
}
