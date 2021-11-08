package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class SerializerWithDefault<T>(valueSerializer: KSerializer<T>, override val default: T) :
    KSerializerWithDefault<T> {

    private val strategy = valueSerializer
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: T) =
        encoder.encodeSerializableValue(strategy, value)

    override fun deserialize(decoder: Decoder): T =
        decoder.decodeSerializableValue(strategy)
}
