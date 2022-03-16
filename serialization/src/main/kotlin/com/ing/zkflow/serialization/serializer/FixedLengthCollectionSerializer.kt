package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import com.ing.zkflow.serialization.FixedLengthType
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
            value.extend(maxSize, valueSerializer.default).forEach {
                encodeSerializableValue(valueSerializer, it)
            }
        }

    override fun deserialize(decoder: Decoder): List<T> =
        decoder.run {
            val actualSize = decodeInt()
            (0 until maxSize)
                .map { decodeSerializableValue(valueSerializer) }
                .shrink(actualSize)
        }

    private fun <T> List<T>.extend(newSize: Int, default: T) = when {
        size < newSize -> List(newSize) { if (it < size) { this[it] } else { default } }
        size == newSize -> this
        size > newSize -> error("List size ($size) is greater than $newSize")
        else -> error("List extend: unreachable")
    }

    private fun <T> List<T>.shrink(newSize: Int) = when {
        size < newSize -> error("List size ($size) is smaller than $newSize")
        size == newSize -> this
        size > newSize -> this.subList(0, newSize)
        else -> error("List shrink: unreachable")
    }
}

data class SizeAnnotation(val value: Int) : Annotation
