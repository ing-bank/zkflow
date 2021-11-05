package com.ing.zkflow.serialization.serializer.char

import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import com.ing.zkflow.serialization.utils.isASCII
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * An ASCII Char is encoded with a single byte.
 */
object ASCIICharSerializer : KSerializerWithDefault<Char> {
    override val default = '-'

    override val descriptor: SerialDescriptor = Byte.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Char) {
        require(value.isASCII) { "Value `$value` is not an ASCII character" }

        encoder.encodeByte(value.toInt().toByte())
    }

    override fun deserialize(decoder: Decoder): Char =
        decoder.decodeByte().toInt().toChar()
}
