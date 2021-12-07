package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthListSerializer<T>(
    private val maxSize: Int,
    private val valueSerializer: KSerializer<T>,
    private val itemDefault: T
) : KSerializerWithDefault<List<T>> {
    constructor(maxSize: Int, valueSerializer: KSerializerWithDefault<T>) : this(maxSize, valueSerializer, valueSerializer.default)

    override val default = emptyList<T>()

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BoundedList") {
        element("size", Int.serializer().descriptor)
        element("list", ListSerializer(valueSerializer).descriptor)
        annotations = listOf(SizeAnnotation(maxSize))
    }

    override fun serialize(encoder: Encoder, value: List<T>) =
        encoder.run {
            encodeInt(value.size)
            value.extend(maxSize, itemDefault).forEach {
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
