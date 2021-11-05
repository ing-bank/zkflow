package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.utils.isASCII
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthASCIIStringSerializer(maxLength: Int) : KSerializerWithDefault<String> {
    private val strategy = FixedLengthListSerializer(maxLength, ASCIICharSerializer)

    override val default = ""
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: String) {
        val asList = value.toList()
        require(asList.all { it.isASCII }) {
            "ASCII String must contain only ASCII characters"
        }

        strategy.serialize(encoder, asList)
    }

    override fun deserialize(decoder: Decoder): String =
        strategy.deserialize(decoder).joinToString(separator = "")
}
