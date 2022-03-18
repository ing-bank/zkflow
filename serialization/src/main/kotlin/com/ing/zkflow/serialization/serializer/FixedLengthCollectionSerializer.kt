package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.util.extendTo
import com.ing.zkflow.util.shrinkTo
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal class FixedLengthCollectionSerializer<T>(
    private val maxSize: Int,
    private val valueSerializer: FixedLengthKSerializerWithDefault<T>,
    fixedLengthType: FixedLengthType
) : FixedLengthKSerializerWithDefault<List<T>> {

    override val default = emptyList<T>()

    override val descriptor = FixedLengthSerialDescriptor(
        buildClassSerialDescriptor(fixedLengthType.serialName) {
            element("size", UInt.serializer().descriptor)
            element("values", ListSerializer(valueSerializer).descriptor)
            annotations = listOf(
                SizeAnnotation(maxSize)
            )
        },
        UIntSerializer.descriptor.byteSize + maxSize * valueSerializer.descriptor.byteSize
    )

    override fun serialize(encoder: Encoder, value: List<T>) =
        encoder.run {
            encodeInt(
                value.size.also {
                    require(it >= 0) {
                        "Collection size cannot be negative!"
                    }
                }
            )
            value.extendTo(maxSize, valueSerializer.default).forEach {
                encodeSerializableValue(valueSerializer, it)
            }
        }

    override fun deserialize(decoder: Decoder): List<T> =
        decoder.run {
            val actualSize = decodeInt()
            (0 until maxSize)
                .map { decodeSerializableValue(valueSerializer) }
                .shrinkTo(actualSize)
        }
}

data class SizeAnnotation(val value: Int) : Annotation
