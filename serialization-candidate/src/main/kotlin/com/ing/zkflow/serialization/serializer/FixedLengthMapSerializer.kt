package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthMapSerializer<K : Any, V>(
    maxSize: Int,
    keySerializer: KSerializerWithDefault<K>,
    valueSerializer: KSerializerWithDefault<V>,
) : KSerializerWithDefault<Map<K, V>> {
    override val default = emptyMap<K, V>()

    private val defaultKey = keySerializer.default
    private val defaultValue = valueSerializer.default

    val strategy = FixedLengthListSerializer(
        maxSize,
        PairSerializer(keySerializer, valueSerializer),
        Pair(defaultKey, defaultValue)
    )

    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: Map<K, V>) =
        strategy.serialize(encoder, value.toList())

    override fun deserialize(decoder: Decoder): Map<K, V> =
        strategy.deserialize(decoder).toMap()
}
