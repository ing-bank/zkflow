package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UUIDSerializer : FixedLengthKSerializerWithDefault<UUID> {
    override val default: UUID = UUID(0, 0)

    @Serializable
    private data class Surrogate(
        @Serializable(with = LongSerializer::class) val mostSignificantBits: Long,
        @Serializable(with = LongSerializer::class) val leastSignificantBits: Long
    )

    private val strategy = Surrogate.serializer()
    override val descriptor = strategy.descriptor.toFixedLengthSerialDescriptorOrThrow()

    override fun serialize(encoder: Encoder, value: UUID) {
        val surrogate = Surrogate(value.mostSignificantBits, value.leastSignificantBits)
        encoder.encodeSerializableValue(strategy, surrogate)
    }

    override fun deserialize(decoder: Decoder): UUID =
        with(decoder.decodeSerializableValue(strategy)) {
            UUID(mostSignificantBits, leastSignificantBits)
        }
}
