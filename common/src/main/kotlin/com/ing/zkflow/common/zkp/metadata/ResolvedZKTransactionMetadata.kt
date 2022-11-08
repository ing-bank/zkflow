package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import net.corda.core.contracts.ComponentGroupEnum

data class ResolvedZKTransactionMetadata(
    val commands: List<ResolvedZKCommandMetadata>
) {
    companion object {
        @Suppress("unused") // Will be re-enabled once ZincPoet is used
        private val DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH = "${System.getProperty("user.dir")}/build/zinc/transactions/"

        const val ERROR_NO_COMMANDS = "There should be at least one command in a ZKFlow transaction"
        const val ERROR_COMMAND_NOT_UNIQUE = "Multiple commands of one type found. All commands in a ZKFLow transaction should be unique"
    }

    val inputs = commands.fold(mutableListOf<ZKReference>()) { acc, command -> mergeUtxoVisibility(acc, command.inputs) }
    val references =
        commands.fold(mutableListOf<ZKReference>()) { acc, command -> mergeUtxoVisibility(acc, command.references) }
    val outputs =
        commands.fold(mutableListOf<ZKProtectedComponent>()) { acc, command -> mergeComponentVisibility(acc, command.outputs) }

    init {
        require(commands.isNotEmpty()) { ERROR_NO_COMMANDS }
        require(commands.distinctBy { it.commandKClass }.size == commands.size) { ERROR_COMMAND_NOT_UNIQUE }
    }

    fun verify(ltx: ZKTransactionBuilder) {
        commands.forEach { it.verify(ltx) }
    }

    fun shouldBeVisibleInFilteredComponentGroup(groupIndex: Int, componentIndex: Int): Boolean {
        return when (groupIndex) {
            /*
             * Options:
             * - private == true: not visible
             * - private == false: visible
             * - not mentioned at all: visible
             */
            ComponentGroupEnum.OUTPUTS_GROUP.ordinal -> outputs.find { it.index == componentIndex }?.mustBePrivate()?.not() ?: true
            else -> true // all other groups have visibility 'Public' by default at the moment, may change in future
        }
    }

    private fun mergeComponentVisibility(
        acc: MutableList<ZKProtectedComponent>,
        components: List<ZKProtectedComponent>
    ): MutableList<ZKProtectedComponent> {
        components.forEach { new ->
            val existing = acc.find { it.index == new.index }
            if (existing == null) {
                acc.add(new)
            } else if (new.private) {
                // we choose the most private visibility requested
                acc.remove(existing)
                acc.add(new)
            }
        }
        return acc
    }

    private fun mergeUtxoVisibility(
        acc: MutableList<ZKReference>,
        components: List<ZKReference>
    ): MutableList<ZKReference> {
        components.forEach { new ->
            val existing = acc.find { it.index == new.index }
            if (existing == null) {
                acc.add(new)
            } else if (new.forcePrivate) {
                // we choose the most private visibility requested
                acc.remove(existing)
                acc.add(new)
            }
        }
        return acc
    }
}
