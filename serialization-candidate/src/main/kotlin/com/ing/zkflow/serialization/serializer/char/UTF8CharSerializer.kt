package com.ing.zkflow.serialization.serializer.char

import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import com.ing.zkflow.serialization.serializer.ShortSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * UTF8 chars are serialized as [Char.SIZE_BYTES] bytes, which is a [Short].
 */
object UTF8CharSerializer : KSerializerWithDefault<Char> {
    override val default = '-'

    override val descriptor: SerialDescriptor = ShortSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Char) = encoder.run {
        encodeShort(value.toShort())
    }

    override fun deserialize(decoder: Decoder): Char = decoder.run {
        decodeShort().toChar()
    }
}
