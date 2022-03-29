package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthCollectionSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [String], using UTF-8 encoding. The UTF-8 encoded byte array is restrained by [maxSize].
 */
open class FixedLengthUtf8StringSerializer(private val maxSize: Int) : FixedLengthKSerializerWithDefault<String> {
    private val strategy = FixedLengthCollectionSerializer(maxSize, ByteSerializer, FixedLengthType.UTF8_STRING)

    override val default = ""
    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        if (bytes.size > maxSize) {
            throw IllegalArgumentException("UTF-8 encoding of String `$value` (${bytes.size}) is longer than $maxSize.")
        }
        encoder.encodeSerializableValue(strategy, bytes.toList())
    }

    override fun deserialize(decoder: Decoder): String {
        val bytes = strategy.deserialize(decoder).toByteArray()
        return String(bytes, Charsets.UTF_8)
    }
}
