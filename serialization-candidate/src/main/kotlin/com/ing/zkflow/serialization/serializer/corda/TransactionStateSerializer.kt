package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.Surrogate
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TransactionState
import net.corda.core.identity.Party

class TransactionStateSerializer<T : ContractState>(
    contractStateSerializer: KSerializer<T>,
    notarySerializer: KSerializer<Party>,
    attachmentConstraintSerializer: KSerializer<AttachmentConstraint>
) : SurrogateSerializer<TransactionState<T>, TransactionStateSurrogate<T, Party, AttachmentConstraint>>(
    TransactionStateSurrogate.serializer(
        contractStateSerializer,
        notarySerializer,
        attachmentConstraintSerializer
    ),
    { TransactionStateSurrogate.from(it) }
)

@Suppress("FINAL_UPPER_BOUND")
@Serializable
data class TransactionStateSurrogate<T : ContractState, P : Party, A : AttachmentConstraint>(
    val data: T,
    @Serializable(with = ContractClassName::class) val contract: String,
    val notary: P,
    @Serializable(with = Encumbrance::class) val encumbrance: Int? = null,
    val constraint: A,
) : Surrogate<TransactionState<T>> {
    override fun toOriginal() = TransactionState(data, contract, notary, encumbrance, constraint)

    companion object {
        const val CLASSNAME_MAX_LENGTH = 100

        object ContractClassName : FixedLengthASCIIStringSerializer(CLASSNAME_MAX_LENGTH)
        object Encumbrance : NullableSerializer<Int>(IntSerializer)

        fun <T : ContractState> from(original: TransactionState<T>) = with(original) {
            TransactionStateSurrogate(data, contract, notary, encumbrance, constraint)
        }
    }
}
