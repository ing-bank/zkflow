package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.Surrogate
import com.ing.zkflow.serialization.FixedLengthKSerializer
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Majority of the classes won't be directly serializable and must be serialized
 * via an intermediate class consisting of serializable elements.
 * This serializer specifies how non-natively serializable types must be converted
 * to such intermediate serializable representations.
 *
 * User will have to specify an appropriate surrogate class and a conversion function `toSurrogate`
 * by annotating the type in question with @Converter annotation.
 */
abstract class SurrogateSerializer<T, S : Surrogate<T>>(
    private val strategy: KSerializer<S>,
    val toSurrogate: (T) -> S
) : FixedLengthKSerializer<T> {
    override val descriptor = strategy.descriptor.toFixedLengthSerialDescriptorOrThrow()

    override fun deserialize(decoder: Decoder): T {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return surrogate.toOriginal()
    }

    override fun serialize(encoder: Encoder, value: T) {
        val surrogate = toSurrogate(value)
        encoder.encodeSerializableValue(strategy, surrogate)
    }
}
