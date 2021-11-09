package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * This serializer must be used to attach a default value to any serializer not-implementing
 * KSerializerWithDefault (and thus not defining a default value).
 * Such cases include 3rd party classes, user classes and some native classes.
 * To attach this serializer to a type, it must be annotated with @Default annotation.
 */
abstract class SerializerWithDefault<T>(valueSerializer: KSerializer<T>, override val default: T) :
    KSerializerWithDefault<T> {

    private val strategy = valueSerializer
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: T) =
        encoder.encodeSerializableValue(strategy, value)

    override fun deserialize(decoder: Decoder): T =
        decoder.decodeSerializableValue(strategy)
}
