package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthCollectionSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.charset.Charset

/**
 * Abstract serializer for [String], with configurable encoding scheme.
 *
 * Strings of all encodings are serialized as a ByteArray, consisting of an Int size, followed by [maxBytes] bytes.
 *
 * @param maxBytes The maximum number of bytes allowed in the encoded String.
 * @param fixedLengthType The [FixedLengthType].
 * @param charset The [Charset] to use when encoding the String.
 */
open class AbstractFixedSizeStringSerializer(
    private val maxBytes: Int,
    fixedLengthType: FixedLengthType,
    private val charset: Charset,
) : FixedLengthKSerializerWithDefault<String> {
    private val strategy = FixedLengthCollectionSerializer(maxBytes, ByteSerializer, fixedLengthType)

    override val default = ""
    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: String) {
        require(charset.newEncoder().canEncode(value)) {
            throw IllegalStateException("String `$value` cannot be encoded with ${charset.name()}")
        }
        val bytes = value.toByteArray(charset)
        if (bytes.size > maxBytes) {
            throw IllegalArgumentException("${charset.name()} encoding of String `$value` (${bytes.size}) is longer than $maxBytes.")
        }
        encoder.encodeSerializableValue(strategy, bytes.toList())
    }

    override fun deserialize(decoder: Decoder): String {
        val bytes = strategy.deserialize(decoder).toByteArray()
        return String(bytes, charset)
    }
}
