package com.ing.zknotary.common.transactions

import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState

data class ComponentPadding private constructor(
    val padding: Map<ComponentGroupEnum, Int>,
    val fillerState: ContractState
) {
    data class Builder(
        private var padding: MutableMap<ComponentGroupEnum, Int> = mutableMapOf(),
        private var fillerState: ContractState? = null
    ) {
        fun inputs(n: Int) = apply { padding[ComponentGroupEnum.INPUTS_GROUP] = n }

        fun outputs(n: Int) = apply { padding[ComponentGroupEnum.OUTPUTS_GROUP] = n }

        // fun commands(n: Int)= apply {
        //     onlyNonNegative(n, "commands")
        //     padding[ComponentGroupEnum.COMMANDS_GROUP] = n }
        //
        // fun attachments(n: Int)= apply {
        //     onlyNonNegative(n, "attachments")
        //     padding[ComponentGroupEnum.ATTACHMENTS_GROUP] = n }
        //
        // fun notary(n: Int)= apply {
        //     onlyNonNegative(n, "notary")
        //     padding[ComponentGroupEnum.NOTARY_GROUP] = n }
        //
        // fun timewindow(n: Int)= apply {
        //     onlyNonNegative(n, "timewindow")
        //     padding[ComponentGroupEnum.TIMEWINDOW_GROUP] = n }

        fun signers(n: Int) = apply { padding[ComponentGroupEnum.SIGNERS_GROUP] = n }

        fun references(n: Int) = apply { padding[ComponentGroupEnum.REFERENCES_GROUP] = n }

        // fun parameters(n: Int)= apply {
        //     onlyNonNegative(n, "parameters")
        //     padding[ComponentGroupEnum.PARAMETERS_GROUP] = n }

        fun stateFiller(contractState: ContractState) = apply { fillerState = contractState }

        fun build(): ComponentPadding {
            positive(padding[ComponentGroupEnum.INPUTS_GROUP], "inputs")
            positive(padding[ComponentGroupEnum.OUTPUTS_GROUP], "outputs")
            positive(padding[ComponentGroupEnum.REFERENCES_GROUP], "references")

            nonNull(fillerState, "state")

            return ComponentPadding(padding, fillerState!!)
        }

        private fun positive(value: Int?, that: String) {
            require(value ?: -1 > 0) { "Size of $that group must be defined with a positive number" }
        }

        private fun <T> nonNull(value: T?, that: String) {
            require(value != null) { "Filler $that must be set" }
        }
    }

    fun component(value: ComponentGroupEnum) = padding[value]
}
