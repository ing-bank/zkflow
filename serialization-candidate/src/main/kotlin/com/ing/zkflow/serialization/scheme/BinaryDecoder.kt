package com.ing.zkflow.serialization.scheme

import com.ing.zkflow.serialization.utils.WILL_NOT_IMPLEMENT
import com.ing.zkflow.serialization.utils.binary.Representation
import com.ing.zkflow.serialization.utils.binary.boolean
import com.ing.zkflow.serialization.utils.binary.byte
import com.ing.zkflow.serialization.utils.binary.int
import com.ing.zkflow.serialization.utils.binary.long
import com.ing.zkflow.serialization.utils.binary.representationLength
import com.ing.zkflow.serialization.utils.binary.short
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.DataInput

@Suppress("TooManyFunctions")
internal class BinaryDecoder(private val input: DataInput, private val representation: Representation, var elementsCount: Int = 0) : AbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeSequentially(): Boolean = true

    override fun decodeBoolean(): Boolean = input
        .readFully(Boolean.representationLength(representation))
        .boolean(representation)

    override fun decodeByte(): Byte = input
        .readFully(Byte.representationLength(representation))
        .byte(representation)

    override fun decodeShort(): Short = input
        .readFully(Short.representationLength(representation))
        .short(representation)

    override fun decodeInt(): Int = input
        .readFully(Int.representationLength(representation))
        .int(representation)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        BinaryDecoder(input, representation, descriptor.elementsCount)

    override fun decodeLong(): Long = input
        .readFully(Long.representationLength(representation))
        .long(representation)

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        decodeInt()

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
