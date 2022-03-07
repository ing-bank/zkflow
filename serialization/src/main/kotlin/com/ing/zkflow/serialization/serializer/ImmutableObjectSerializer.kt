package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer targeting immutable objects serialization.
 * Serializes nothing and returns the default value.
 */
class ImmutableObjectSerializer<T : Any>(override val default: T) : FixedLengthKSerializerWithDefault<T> {
    override val descriptor = FixedLengthSerialDescriptor(
        buildClassSerialDescriptor(default::class.qualifiedName ?: error("Cannot deduce qualified name")),
        0
    )
    override fun serialize(encoder: Encoder, value: T) = Unit
    override fun deserialize(decoder: Decoder) = default
}
