package com.ing.zkflow.serialization.serializer.char

import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.ByteBuffer

/**
 * UTF8 chars are serialized as Char.SIZE_BYTES bytes.
 */
object UTF8CharSerializer : KSerializerWithDefault<Char> {
    override val default = '-'

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UTF8Char") {
        element("0", Byte.serializer().descriptor)
        element("1", Byte.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: Char) = encoder.run {
        ByteBuffer
            .allocate(Char.SIZE_BYTES)
            .putChar(value)
            .array()
            .let { it.forEach { byte -> encodeByte(byte) } }
    }

    override fun deserialize(decoder: Decoder): Char = decoder.run {
        (0 until Char.SIZE_BYTES)
            .map { decodeByte() }
            .let { dec ->
                ByteBuffer.wrap(dec.toByteArray()).char
            }
    }
}
