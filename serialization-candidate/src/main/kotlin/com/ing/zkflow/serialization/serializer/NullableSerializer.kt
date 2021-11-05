package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class NullableSerializer<T>(valueSerializer: KSerializerWithDefault<T>) : KSerializerWithDefault<T?> {
    @Serializable
    private data class Surrogate<T>(
        val isNull: Boolean,
        val value: T
    )

    override val default = valueSerializer.default

    private val strategy = Surrogate.serializer(valueSerializer)
    override val descriptor: SerialDescriptor = strategy.descriptor.nullable

    override fun serialize(encoder: Encoder, value: T?) =
        encoder.encodeSerializableValue(strategy, Surrogate(value == null, value ?: default))

    override fun deserialize(decoder: Decoder): T? =
        decoder.decodeSerializableValue(strategy).let {
            if (it.isNull) null else it.value
        }
}
