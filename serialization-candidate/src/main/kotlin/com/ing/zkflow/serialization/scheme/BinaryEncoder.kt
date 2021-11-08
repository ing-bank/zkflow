package com.ing.zkflow.serialization.scheme

import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer.DoubleSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer.FloatSerializer
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.char.UTF8CharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
import com.ing.zkflow.serialization.utils.WILL_NOT_IMPLEMENT
import com.ing.zkflow.serialization.utils.binary.Representation
import com.ing.zkflow.serialization.utils.binary.binary
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.DataOutput

@Suppress("TooManyFunctions")
internal class BinaryEncoder(
    private val output: DataOutput,
    private val representation: Representation
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun encodeBoolean(value: Boolean) =
        output.write(value.binary(representation))

    override fun encodeByte(value: Byte) =
        output.write(value.binary(representation))

    override fun encodeShort(value: Short) =
        output.write(value.binary(representation))

    override fun encodeInt(value: Int) =
        output.write(value.binary(representation))

    override fun encodeLong(value: Long) =
        output.write(value.binary(representation))

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
        encodeInt(index)

    override fun encodeFloat(value: Float) {
        WILL_NOT_IMPLEMENT("To encode Float, use ${FloatSerializer::class.qualifiedName}")
    }
    override fun encodeDouble(value: Double) {
        WILL_NOT_IMPLEMENT("To encode Double, use ${DoubleSerializer::class.qualifiedName}")
    }
    override fun encodeChar(value: Char) {
        WILL_NOT_IMPLEMENT("To encode Char, use ${ASCIICharSerializer::class.qualifiedName} or ${UTF8CharSerializer::class.qualifiedName}")
    }
    override fun encodeString(value: String) {
        WILL_NOT_IMPLEMENT("To encode Char, use ${FixedLengthASCIIStringSerializer::class.qualifiedName} or ${FixedLengthUTF8StringSerializer::class.qualifiedName}")
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
