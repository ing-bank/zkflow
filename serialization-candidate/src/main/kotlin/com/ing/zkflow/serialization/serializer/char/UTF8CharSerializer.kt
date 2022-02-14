package com.ing.zkflow.serialization.serializer.char

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.ShortSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * UTF8 chars are serialized as [Char.SIZE_BYTES] bytes, which is a [Short].
 */
object UTF8CharSerializer : FixedLengthKSerializerWithDefault<Char> {
    override val default = '-'

    val strategy = ShortSerializer
    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: Char) = encoder.run {
        encodeShort(value.toShort())
    }

    override fun deserialize(decoder: Decoder): Char = decoder.run {
        decodeShort().toChar()
    }
}
