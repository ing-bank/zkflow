package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import com.ing.zkflow.serialization.FixedLengthType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A [FixedLengthKSerializerWithDefault] for [List]s that are known to have exactly [exactSize] elements.
 * The difference to [FixedLengthCollectionSerializer] is that this serializer will not write [List.size] to the
 * serialized form, as it is expected to be known.
 */
internal class ExactLengthCollectionSerializer<T>(
    private val exactSize: Int,
    private val valueSerializer: FixedLengthKSerializerWithDefault<T>,
    fixedLengthType: FixedLengthType
) : FixedLengthKSerializerWithDefault<List<T>> {

    override val default = List(exactSize) {
        valueSerializer.default
    }

    override val descriptor = FixedLengthSerialDescriptor(
        buildClassSerialDescriptor(fixedLengthType.serialName) {
            element("values", ListSerializer(valueSerializer).descriptor)
            annotations = listOf(
                SizeAnnotation(exactSize)
            )
        },
        exactSize * valueSerializer.descriptor.byteSize
    )

    override fun serialize(encoder: Encoder, value: List<T>) {
        require(value.size == exactSize) {
            "Expected a collection of size $exactSize, but got a collection of ${value.size}."
        }
        encoder.run {
            value.forEach {
                encodeSerializableValue(valueSerializer, it)
            }
        }
    }

    override fun deserialize(decoder: Decoder): List<T> =
        decoder.run {
            (0 until exactSize)
                .map { decodeSerializableValue(valueSerializer) }
        }
}
