package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer targeting immutable objects serialization.
 * Serializes nothing and returns the default value.
 */
class ImmutableObjectSerializer<T : Any>(override val default: T) : KSerializerWithDefault<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(default::class.qualifiedName ?: error("Cannot deduce qualified name"))
    override fun serialize(encoder: Encoder, value: T) = Unit
    override fun deserialize(decoder: Decoder) = default
}
