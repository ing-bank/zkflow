package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthListSerializer<T>(
    maxSize: Int,
    valueSerializer: KSerializer<T>,
    itemDefault: T,
) : KSerializerWithDefault<List<T>> {
    constructor(maxSize: Int, valueSerializer: KSerializerWithDefault<T>) : this(maxSize, valueSerializer, valueSerializer.default)

    private val strategy = FixedLengthCollectionSerializer(maxSize, valueSerializer, itemDefault, FixedLengthType.LIST)
    override val descriptor: SerialDescriptor = strategy.descriptor
    override val default: List<T> = emptyList()

    override fun deserialize(decoder: Decoder): List<T> {
        return strategy.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        return strategy.serialize(encoder, value)
    }
}
