package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TransactionState
import net.corda.core.identity.Party

class TransactionStateSerializer<T : ContractState>(contractStateSerializer: KSerializer<T>) :
    SurrogateSerializer<TransactionState<T>, TransactionStateSurrogate<T>>(
        TransactionStateSurrogate.serializer(contractStateSerializer),
        { TransactionStateSurrogate.from(it) }
    )

@Serializable
data class TransactionStateSurrogate<T : ContractState>(
    val data: @Contextual T,
    @FixedLength([256]) val contract: ContractClassName,
    val notary: @Contextual Party,
    val encumbrance: Int? = null,
    // TODO only supports few of them. See AttachmentConstraintSerializer.kt.
    val constraint: AttachmentConstraint
) : Surrogate<TransactionState<T>> {
    override fun toOriginal() = TransactionState(data, contract, notary, encumbrance, constraint)

    companion object {
        fun <T : ContractState> from(original: TransactionState<T>) = with(original) {
            TransactionStateSurrogate(data, contract, notary, encumbrance, constraint)
        }
    }
}
