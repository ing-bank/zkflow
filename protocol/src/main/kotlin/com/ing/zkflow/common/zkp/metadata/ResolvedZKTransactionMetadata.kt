package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.transactions.LedgerTransaction
import kotlin.reflect.KClass

data class ResolvedZKTransactionMetadata(
    val commands: List<ResolvedZKCommandMetadata>
) {
    companion object {
        @Suppress("unused") // Will be re-enabled once ZincPoet is used
        private val DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH = "${System.getProperty("user.dir")}/build/zinc/transactions/"

        const val ERROR_NO_COMMANDS = "There should be at least one commmand in a ZKFlow transaction"
        const val ERROR_COMMAND_NOT_UNIQUE = "Multiple commands of one type found. All commands in a ZKFLow transaction should be unique"
    }

    val inputs = commands.fold(mutableListOf<ZKReference>()) { acc, command -> mergeUtxoVisibility(acc, command.inputs) }
    val references =
        commands.fold(mutableListOf<ZKReference>()) { acc, command -> mergeUtxoVisibility(acc, command.references) }
    val outputs =
        commands.fold(mutableListOf<ZKProtectedComponent>()) { acc, command -> mergeComponentVisibility(acc, command.outputs) }

    /**
     * The total number of signers of all commands added up.
     *
     * In theory, they may overlap (be the same PublicKeys), but we can't determine that easily.
     * Possible future optimization.
     */
    val numberOfSigners: Int by lazy { commands.sumOf { it.numberOfSigners } }

    val hasTimeWindow: Boolean = commands.any { it.timeWindow }

    /**
     * The aggregate list of java class to zinc type for all commands in this transaction.
     */
    val javaClass2ZincType: Map<KClass<out ContractState>, ZincType> by lazy {
        commands.fold(mapOf()) { acc, resolvedZKCommandMetadata ->
            acc + resolvedZKCommandMetadata.circuit.javaClass2ZincType
        }
    }

    init {
        require(commands.isNotEmpty()) { ERROR_NO_COMMANDS }
        require(commands.distinctBy { it.commandKClass }.size == commands.size) { ERROR_COMMAND_NOT_UNIQUE }
        // TODO        TODO("TODO: Verify components visibility is aligned among different commands")
    }

    fun verify(ltx: LedgerTransaction) {
        commands.forEach { it.verify(ltx) }
    }

    fun verify(ltx: ZKTransactionBuilder) {
        commands.forEach { it.verify(ltx) }
    }

    fun isVisibleInFilteredComponentGroup(groupIndex: Int, componentIndex: Int): Boolean {
        return when (groupIndex) {
            /*
             * Options:
             * - private == true: not visible
             * - private == false: visible
             * - not mentioned at all: visible
             */
            ComponentGroupEnum.OUTPUTS_GROUP.ordinal -> outputs.find { it.index == componentIndex }?.private?.not() ?: true
            else -> true // all other groups have visibility 'Public' by default at the moment, may change in future
        }
    }

    /**
     * If a component is mentioned in any way in the metadata, it should be present in the witness.
     * Otherwise, it will not be present in the witness.
     */
    fun isVisibleInWitness(groupIndex: Int, componentIndex: Int): Boolean {
        return when (groupIndex) {
            ComponentGroupEnum.INPUTS_GROUP.ordinal -> inputs.any { it.index == componentIndex } // Here we return UTXO visibility, not StateRef visibility (StateRefs never go to witness)
            ComponentGroupEnum.REFERENCES_GROUP.ordinal -> references.any { it.index == componentIndex } // Here we return UTXO visibility, not StateRef visibility (StateRefs never go to witness)
            ComponentGroupEnum.OUTPUTS_GROUP.ordinal -> outputs.any { it.index == componentIndex }
            else -> false // other groups are not part of the witness for now, may change in the future.
        }
    }

    // TODO: can this be deleted?
    fun isOnlyPrivateUtxoAllowed(groupIndex: Int, componentIndex: Int): Boolean {
        return when (groupIndex) {
            ComponentGroupEnum.INPUTS_GROUP.ordinal -> inputs.find { it.index == componentIndex }?.forcePrivate
                ?: false // We don't care about utxo visibility unless specifically marked with 'forcePrivate'
            ComponentGroupEnum.REFERENCES_GROUP.ordinal -> references.find { it.index == componentIndex }?.forcePrivate
                ?: false // We don't care about utxo visibility unless specifically marked with 'forcePrivate'
            else -> error("Only Inputs and References UTXOs are allowed")
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
