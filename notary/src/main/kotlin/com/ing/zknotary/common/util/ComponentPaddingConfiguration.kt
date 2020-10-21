package com.ing.zknotary.common.util

import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.zkp.ZKNulls
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class ComponentPaddingConfiguration private constructor(
    private val padding: Map<Int, Int>,
    private val fillers: Map<Int, Filler>
) {

    @CordaSerializable
    sealed class Filler {
        data class TransactionState(val content: net.corda.core.contracts.TransactionState<ZKContractState>) : Filler()
        data class StateAndRef(val content: net.corda.core.contracts.StateAndRef<ContractState>) : Filler()
        data class StateRef(val content: net.corda.core.contracts.StateRef) : Filler()
        data class PublicKey(val content: java.security.PublicKey) : Filler()
        data class SecureHash(val content: net.corda.core.crypto.SecureHash) : Filler()
    }

    class Builder(
        private var padding: MutableMap<Int, Int> = mutableMapOf(),
        private var fillers: MutableMap<Int, Filler> = mutableMapOf()
    ) {
        fun inputs(n: Int, filler: Filler) = apply {
            padding[ComponentGroupEnum.INPUTS_GROUP.ordinal] = n
            fillers[ComponentGroupEnum.INPUTS_GROUP.ordinal] = filler
        }

        fun outputs(n: Int, filler: Filler) = apply {
            padding[ComponentGroupEnum.OUTPUTS_GROUP.ordinal] = n
            fillers[ComponentGroupEnum.OUTPUTS_GROUP.ordinal] = filler
        }

        fun signers(n: Int, filler: Filler = Filler.PublicKey(ZKNulls.NULL_PUBLIC_KEY)) = apply {
            padding[ComponentGroupEnum.SIGNERS_GROUP.ordinal] = n
            fillers[ComponentGroupEnum.SIGNERS_GROUP.ordinal] = filler
        }

        fun references(n: Int, filler: Filler) = apply {
            padding[ComponentGroupEnum.REFERENCES_GROUP.ordinal] = n
            fillers[ComponentGroupEnum.REFERENCES_GROUP.ordinal] = filler
        }

        fun attachments(n: Int, filler: Filler) = apply {
            padding[ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal] = n
            fillers[ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal] = filler
        }

        fun build(): ComponentPaddingConfiguration {
            nonNegative(padding[ComponentGroupEnum.INPUTS_GROUP.ordinal], "inputs")
            nonNegative(padding[ComponentGroupEnum.OUTPUTS_GROUP.ordinal], "outputs")
            nonNegative(padding[ComponentGroupEnum.REFERENCES_GROUP.ordinal], "references")
            nonNegative(padding[ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal], "attachments")

            return ComponentPaddingConfiguration(padding, fillers)
        }

        // This function must only be used for testing.
        // It is expected that padding will become obsolete in the near future.
        // If padding is required past
        // ** 01.02.2021 **
        // this functionality must refactored.
        internal fun empty(): ComponentPaddingConfiguration {
            return ComponentPaddingConfiguration(padding, fillers)
        }

        private fun nonNegative(value: Int?, that: String) {
            require(value ?: -1 >= 0) { "Size of $that group must be defined with a non-negative number" }
        }
    }

    fun sizeOf(componentGroup: ComponentGroupEnum) = padding[componentGroup.ordinal]
    fun filler(componentGroup: ComponentGroupEnum) = fillers[componentGroup.ordinal]

    fun validate(ptx: ZKProverTransaction) {
        requireThat {
            "Inputs' size exceeds quantity accepted by ZK circuit" using
                (ptx.inputs.size <= sizeOf(ComponentGroupEnum.INPUTS_GROUP) ?: ptx.inputs.size - 1)

            "Outputs' size exceeds quantity accepted by ZK circuit" using
                (ptx.outputs.size <= sizeOf(ComponentGroupEnum.OUTPUTS_GROUP) ?: ptx.outputs.size - 1)

            "References' size exceeds quantity accepted by ZK circuit" using
                (ptx.references.size <= sizeOf(ComponentGroupEnum.REFERENCES_GROUP) ?: ptx.references.size - 1)

            "Attachments' size exceeds quantity accepted by ZK circuit" using
                (ptx.attachments.size <= sizeOf(ComponentGroupEnum.ATTACHMENTS_GROUP) ?: ptx.attachments.size - 1)

            "Signers' size exceeds quantity accepted by ZK circuit" using
                (ptx.command.signers.size <= sizeOf(ComponentGroupEnum.SIGNERS_GROUP) ?: ptx.command.signers.size - 1)
        }
    }

    fun validate(vtx: ZKVerifierTransaction) {
        requireThat {
            "Inputs' size exceeds quantity accepted by ZK circuit" using (
                vtx.inputs.size <= sizeOf(ComponentGroupEnum.INPUTS_GROUP) ?: vtx.inputs.size - 1
                )

            "Outputs' size exceeds quantity accepted by ZK circuit" using (
                vtx.outputHashes.size <= sizeOf(ComponentGroupEnum.OUTPUTS_GROUP) ?: (vtx.outputHashes.size) - 1
                )

            "References' size exceeds quantity accepted by ZK circuit" using (
                vtx.references.size <= sizeOf(ComponentGroupEnum.REFERENCES_GROUP) ?: vtx.references.size - 1
                )
        }
    }
}
