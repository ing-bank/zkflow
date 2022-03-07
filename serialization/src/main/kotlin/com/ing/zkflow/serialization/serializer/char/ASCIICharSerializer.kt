package com.ing.zkflow.serialization.serializer.char

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.ByteSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * An ASCII Char is encoded with a single byte.
 */
object ASCIICharSerializer : FixedLengthKSerializerWithDefault<Char> {
    // Default value for an ASCII char can be any readable ASCII character.
    override val default = '-'

    val strategy = ByteSerializer
    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: Char) {
        require(supports(value)) { "Value `$value` is not an ASCII character" }

        encoder.encodeByte(value.toInt().toByte())
    }

    override fun deserialize(decoder: Decoder): Char =
        decoder.decodeByte().toInt().toChar()

    private const val MAX_ASCII_CODE = 255

    fun supports(char: Char): Boolean =
        char.toInt() in 0..MAX_ASCII_CODE
}
