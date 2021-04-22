package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TransactionState
import net.corda.core.identity.Party

class TransactionStateSerializer<T : ContractState>(contractStateSerializer: KSerializer<T>) : KSerializer<TransactionState<T>> {
    private val strategy = TransactionStateSurrogate.serializer(contractStateSerializer)
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): TransactionState<T> {
        return decoder.decodeSerializableValue(strategy).toOriginal()
    }

    override fun serialize(encoder: Encoder, value: TransactionState<T>) {
        encoder.encodeSerializableValue(strategy, TransactionStateSurrogate.from(value))
    }
}

@Serializable
data class TransactionStateSurrogate<T : ContractState>(
    val data: @Contextual T,
    @FixedLength([256]) val contract: ContractClassName,
    val notary: @Contextual Party,
    val encumbrance: Int? = null,
    // TODO only supports few of them. See AttachmentConstraintSerializer.kt.
    val constraint: AttachmentConstraint
) {
    fun toOriginal() = TransactionState(data, contract, notary, encumbrance, constraint)

    companion object {
        fun <T : ContractState> from(original: TransactionState<T>) = with(original) {
            TransactionStateSurrogate(data, contract, notary, encumbrance, constraint)
        }
    }
}
