package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthCollectionSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthStringSerializer(private val maxLength: Int) : FixedLengthKSerializerWithDefault<String> {
    private val strategy = FixedLengthCollectionSerializer(maxLength, ByteSerializer, FixedLengthType.STRING)

    override val default = ""
    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        if (bytes.size > maxLength) {
            throw IllegalArgumentException("String `$value` (${bytes.size}) is longer than $maxLength")
        }
        encoder.encodeSerializableValue(strategy, bytes.toList())
    }

    override fun deserialize(decoder: Decoder): String {
        val bytes = strategy.deserialize(decoder).toByteArray()
        return String(bytes, Charsets.UTF_8)
    }
}
