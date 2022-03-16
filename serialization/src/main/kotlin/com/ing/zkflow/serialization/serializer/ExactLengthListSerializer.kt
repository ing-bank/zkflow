package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthType
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class ExactLengthListSerializer<T>(
    exactSize: Int,
    valueSerializer: FixedLengthKSerializerWithDefault<T>,
) : FixedLengthKSerializerWithDefault<List<T>> {
    private val strategy = ExactLengthCollectionSerializer(exactSize, valueSerializer, FixedLengthType.EXACT_LIST)

    override val descriptor = strategy.descriptor
    override val default: List<T> = strategy.default

    override fun deserialize(decoder: Decoder): List<T> {
        return strategy.deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        return strategy.serialize(encoder, value)
    }
}
