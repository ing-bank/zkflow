package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UUIDSerializer : KSerializerWithDefault<UUID> {
    override val default: UUID = UUID(0, 0)

    @Serializable
    private data class Surrogate(
        val mostSignificantBits: @Serializable(with = LongSerializer::class) Long,
        val leastSignificantBits: @Serializable(with = LongSerializer::class) Long
    )

    private val strategy = Surrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: UUID) {
        val surrogate = Surrogate(value.mostSignificantBits, value.leastSignificantBits)
        encoder.encodeSerializableValue(strategy, surrogate)
    }

    override fun deserialize(decoder: Decoder): UUID =
        with(decoder.decodeSerializableValue(strategy)) {
            UUID(mostSignificantBits, leastSignificantBits)
        }
}
