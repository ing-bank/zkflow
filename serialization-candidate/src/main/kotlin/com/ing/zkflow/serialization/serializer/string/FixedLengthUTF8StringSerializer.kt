package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.serialization.serializer.FixedLengthCollectionSerializer
import com.ing.zkflow.serialization.serializer.char.UTF8CharSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A UTF-8 string is serialized as list of UTF Chars
 */
open class FixedLengthUTF8StringSerializer(private val maxLength: Int) : FixedLengthKSerializerWithDefault<String> {
    override val default = ""

    private val strategy = FixedLengthCollectionSerializer(maxLength, UTF8CharSerializer, FixedLengthType.UTF8_STRING)
    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: String) {
        try {
            encoder.encodeSerializableValue(strategy, value.toList())
        } catch (e: Error) {
            throw IllegalArgumentException("String `$value` is longer than $maxLength", e)
        }
    }

    override fun deserialize(decoder: Decoder): String =
        strategy.deserialize(decoder).joinToString(separator = "")
}
