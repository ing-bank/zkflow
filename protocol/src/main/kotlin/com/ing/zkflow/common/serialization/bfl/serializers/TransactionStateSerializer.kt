package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zkflow.common.serialization.bfl.serializers.CordaSerializers.CLASS_NAME_SIZE
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TransactionState
import net.corda.core.identity.Party

class TransactionStateSerializer<T : ContractState>(contractStateSerializer: KSerializer<T>) :
    SurrogateSerializer<TransactionState<T>, TransactionStateSurrogate<T>>(
        TransactionStateSurrogate.serializer(contractStateSerializer),
        { TransactionStateSurrogate.from(it) }
    )

@Serializable
@Suppress("ArrayInDataClass")
data class TransactionStateSurrogate<T : ContractState>(
    val contractState: @Contextual T,
    @FixedLength([CLASS_NAME_SIZE])
    val contractClassName: ByteArray,
    val notary: @Contextual Party,
    val encumbrance: Int? = null,
    val constraint: AttachmentConstraint,
) : Surrogate<TransactionState<T>> {
    override fun toOriginal() = TransactionState(contractState, contractClassName.getOriginalClassname(), notary, encumbrance, constraint)

    companion object {
        fun <T : ContractState> from(original: TransactionState<T>) = with(original) {
            TransactionStateSurrogate(data, contract.toBytes(), notary, encumbrance, constraint)
        }
    }
}
