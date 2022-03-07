package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.Surrogate
import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash

class StateRefSerializer(txhashSerializer: SecureHashSerializer) : FixedLengthKSerializerWithDefault<StateRef> {

    @Serializable
    data class StateRefSurrogate<S : SecureHash>(
        val txhash: S,
        @Serializable(with = IntSerializer::class) val index: Int
    ) : Surrogate<StateRef> {
        override fun toOriginal() = StateRef(txhash, index)
    }

    override val default = StateRef(txhashSerializer.default, IntSerializer.default)

    private val strategy = StateRefSurrogate.serializer(txhashSerializer)
    override val descriptor = strategy.descriptor.toFixedLengthSerialDescriptorOrThrow()

    override fun serialize(encoder: Encoder, value: StateRef) =
        encoder.encodeSerializableValue(strategy, StateRefSurrogate(value.txhash, value.index))

    override fun deserialize(decoder: Decoder) =
        decoder.decodeSerializableValue(strategy).toOriginal()
}
