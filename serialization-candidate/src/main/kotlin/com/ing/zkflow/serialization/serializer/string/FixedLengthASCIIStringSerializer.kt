package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthASCIIStringSerializer(private val maxLength: Int) : KSerializerWithDefault<String> {
    private val strategy = FixedLengthListSerializer(maxLength, ASCIICharSerializer)

    override val default = ""
    override val descriptor: SerialDescriptor = strategy.descriptor

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
