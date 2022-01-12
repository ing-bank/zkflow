package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

object InstantSerializer : KSerializerWithDefault<Instant> {
    override val default: Instant = Instant.MIN

    @Serializable
    private data class Surrogate(
        val seconds: @Serializable(with = LongSerializer::class) Long,
        val nanos: @Serializable(with = IntSerializer::class) Int
    )

    private val strategy = Surrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: Instant) {
        val surrogate = Surrogate(value.epochSecond, value.nano)
        encoder.encodeSerializableValue(strategy, surrogate)
    }

    override fun deserialize(decoder: Decoder): Instant =
        with(decoder.decodeSerializableValue(strategy)) {
            Instant.ofEpochSecond(seconds, nanos.toLong())
        }
}
