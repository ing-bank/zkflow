package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthType
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthMapSerializer<K : Any, V>(
    maxSize: Int,
    keySerializer: FixedLengthKSerializerWithDefault<K>,
    valueSerializer: FixedLengthKSerializerWithDefault<V>,
) : FixedLengthKSerializerWithDefault<Map<K, V>> {
    override val default = emptyMap<K, V>()

    private val strategy = FixedLengthCollectionSerializer(
        maxSize,
        PairSerializerWithDefault(keySerializer, valueSerializer),
        FixedLengthType.MAP
    )

    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: Map<K, V>) =
        strategy.serialize(encoder, value.toList())

    override fun deserialize(decoder: Decoder): Map<K, V> =
        strategy.deserialize(decoder).toMap()
}
