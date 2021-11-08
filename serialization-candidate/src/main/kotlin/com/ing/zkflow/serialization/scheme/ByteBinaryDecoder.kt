package com.ing.zkflow.serialization.scheme

import com.ing.zkflow.serialization.utils.WILL_NOT_IMPLEMENT
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.DataInput
import java.nio.ByteBuffer

@Suppress("TooManyFunctions")
internal class ByteBinaryDecoder(private val input: DataInput) : AbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeSequentially(): Boolean = true

    override fun decodeBoolean(): Boolean = input
        .readFully(1)
        .last() == 1.toByte()

    override fun decodeByte(): Byte = input
        .readFully(Byte.SIZE_BYTES)
        .last()

    override fun decodeShort(): Short = input
        .readFully(Short.SIZE_BYTES)
        .let {
            ByteBuffer.wrap(it).short
        }

    override fun decodeInt(): Int = input
        .readFully(Int.SIZE_BYTES)
        .let {
            ByteBuffer.wrap(it).int
        }

    override fun decodeLong(): Long = input
        .readFully(Long.SIZE_BYTES)
        .let {
            ByteBuffer.wrap(it).long
        }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        decodeInt()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ByteBinaryDecoder(input)

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

    private fun DataInput.readFully(n: Int): ByteArray {
        val buffer = ByteArray(n)
        readFully(buffer)
        return buffer
    }
}
