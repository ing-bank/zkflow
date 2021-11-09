package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.UTF8CharSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A UTF-8 string is serialized as list of UTF Chars
 */
open class FixedLengthUTF8StringSerializer(private val maxLength: Int) : KSerializerWithDefault<String> {
    override val default = ""

    private val strategy = FixedLengthListSerializer(maxLength, UTF8CharSerializer)
    override val descriptor: SerialDescriptor = strategy.descriptor

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
