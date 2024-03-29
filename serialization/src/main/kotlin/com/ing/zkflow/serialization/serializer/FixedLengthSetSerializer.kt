package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthType
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthSetSerializer<T : Any>(
    maxSize: Int,
    valueSerializer: FixedLengthKSerializerWithDefault<T>,
) : FixedLengthKSerializerWithDefault<Set<T>> {
    /**
     * Sets must be serialized as lists to achieve fixed length, because maps may not contain duplicate keys.
     */

    override val default = emptySet<T>()

    private val strategy = FixedLengthCollectionSerializer(maxSize, valueSerializer, FixedLengthType.SET)

    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: Set<T>) =
        strategy.serialize(encoder, value.toList())

    override fun deserialize(decoder: Decoder): Set<T> =
        strategy.deserialize(decoder).toSet()
}
