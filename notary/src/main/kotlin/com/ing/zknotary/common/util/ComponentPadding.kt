package com.ing.zknotary.common.util

import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.zkp.ZKNulls
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class ComponentPadding private constructor(
    private val padding: Map<Int, Int>,
    private val fillers: Map<Int, Filler>
) {
    @CordaSerializable
    sealed class Filler {
        data class ZKStateAndRef(val value: com.ing.zknotary.common.states.ZKStateAndRef<ContractState>) : Filler()
        data class ZKStateRef(val value: com.ing.zknotary.common.states.ZKStateRef): Filler()
        data class PublicKey(val value: java.security.PublicKey) : Filler()
    }

    data class Builder(
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

        fun build(): ComponentPadding {
            positive(padding[ComponentGroupEnum.INPUTS_GROUP.ordinal], "inputs")
            positive(padding[ComponentGroupEnum.OUTPUTS_GROUP.ordinal], "outputs")
            positive(padding[ComponentGroupEnum.REFERENCES_GROUP.ordinal], "references")

            return ComponentPadding(padding, fillers)
        }

        private fun positive(value: Int?, that: String) {
            require(value ?: -1 > 0) { "Size of $that group must be defined with a positive number" }
        }
    }

    fun sizeOf(componentGroup: ComponentGroupEnum) = padding[componentGroup.ordinal]
    fun filler(componentGroup: ComponentGroupEnum) = fillers[componentGroup.ordinal]

    fun validate(ptx: ZKProverTransaction) {
        requireThat {
            "Inputs' size exceeds quantity accepted by ZK circuit" using (
                ptx.inputs.size <= sizeOf(ComponentGroupEnum.INPUTS_GROUP) ?: ptx.inputs.size - 1
                )

            "Outputs' size exceeds quantity accepted by ZK circuit" using (
                ptx.outputs.size <= sizeOf(ComponentGroupEnum.OUTPUTS_GROUP) ?: ptx.outputs.size - 1
                )

            "References' size exceeds quantity accepted by ZK circuit" using (
                ptx.references.size <= sizeOf(ComponentGroupEnum.REFERENCES_GROUP) ?: ptx.references.size - 1
                )

            "Signers' size exceeds quantity accepted by ZK circuit" using (
                ptx.command.signers.size <= sizeOf(ComponentGroupEnum.SIGNERS_GROUP) ?: ptx.command.signers.size - 1
                )
        }
    }

    fun validate(vtx: ZKVerifierTransaction) {
        requireThat {
            "Inputs' size exceeds quantity accepted by ZK circuit" using (
                vtx.inputs.size <= sizeOf(ComponentGroupEnum.INPUTS_GROUP) ?: vtx.inputs.size - 1
                )

            "Outputs' size exceeds quantity accepted by ZK circuit" using (
                vtx.outputs.size <= sizeOf(ComponentGroupEnum.OUTPUTS_GROUP) ?: vtx.outputs.size - 1
                )

            "References' size exceeds quantity accepted by ZK circuit" using (
                vtx.references.size <= sizeOf(ComponentGroupEnum.REFERENCES_GROUP) ?: vtx.references.size - 1
                )
        }
    }
}
