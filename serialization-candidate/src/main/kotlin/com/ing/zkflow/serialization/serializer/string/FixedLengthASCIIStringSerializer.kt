package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.serialization.serializer.FixedLengthCollectionSerializer
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthASCIIStringSerializer(private val maxLength: Int) : FixedLengthKSerializerWithDefault<String> {
    private val strategy = FixedLengthCollectionSerializer(maxLength, ASCIICharSerializer, FixedLengthType.ASCII_STRING)

    override val default = ""
    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: String) {
        val asList = value.toList()

        asList.filterNot { ASCIICharSerializer.supports(it) }.let { nonASCII ->
            require(nonASCII.isEmpty()) {
                "String to be encoded as ASCII `$value` contains non ASCII characters: ${nonASCII.joinToString(separator = ", "){ "`$it`" }} "
            }
        }

        try {
            encoder.encodeSerializableValue(strategy, asList)
        } catch (e: Error) {
            throw IllegalArgumentException("String `$value` is longer than $maxLength", e)
        }
    }

    override fun deserialize(decoder: Decoder): String =
        strategy.deserialize(decoder).joinToString(separator = "")
}
