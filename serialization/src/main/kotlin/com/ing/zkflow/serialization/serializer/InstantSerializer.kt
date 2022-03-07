package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

object InstantSerializer : FixedLengthKSerializerWithDefault<Instant> {
    override val default: Instant = Instant.MIN

    @Serializable
    private data class Surrogate(
        @Serializable(with = LongSerializer::class) val seconds: Long,
        @Serializable(with = IntSerializer::class) val nanos: Int
    )

    private val strategy = Surrogate.serializer()
    override val descriptor = strategy.descriptor.toFixedLengthSerialDescriptorOrThrow()

    override fun serialize(encoder: Encoder, value: Instant) {
        val surrogate = Surrogate(value.epochSecond, value.nano)
        encoder.encodeSerializableValue(strategy, surrogate)
    }

    override fun deserialize(decoder: Decoder): Instant =
        with(decoder.decodeSerializableValue(strategy)) {
            Instant.ofEpochSecond(seconds, nanos.toLong())
        }
}
