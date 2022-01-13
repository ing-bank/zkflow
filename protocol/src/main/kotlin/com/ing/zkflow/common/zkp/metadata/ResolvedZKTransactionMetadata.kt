package com.ing.zkflow.common.zkp.metadata

import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.transactions.LedgerTransaction
import kotlin.reflect.KClass

@Suppress("TooManyFunctions") // TODO: Fix this once we agree on design
data class ResolvedZKTransactionMetadata(
    val commands: List<ResolvedZKCommandMetadata>
) {
    companion object {
        @Suppress("unused") // Will be re-enabled once ZincPoet is used
        private val DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH = "${System.getProperty("user.dir")}/build/zinc/transactions/"

        const val ERROR_NO_COMMANDS = "There should be at least one commmand in a ZKFlow transaction"
        const val ERROR_COMMAND_NOT_UNIQUE = "Multiple commands of one type found. All commands in a ZKFLow transaction should be unique"
        const val ERROR_NETWORKS_DO_NOT_MATCH = "All commands should belong to the same ZKNetwork"
        const val ERROR_ATTACHMENT_CONSTRAINT_DOES_NOT_MATCH = "The attachment constraint of all commands should match the network"
    }

    private fun verifyCommandsMatchNetwork() {
        require(commands.map { it.network }.distinct().size == 1) { ERROR_NETWORKS_DO_NOT_MATCH }
    }

    val privateInputs = commands.fold(mutableListOf<ZKProtectedComponent>()) { acc, command -> mergeComponentVisibility(acc, command.privateInputs) }
    val privateReferences = commands.fold(mutableListOf<ZKProtectedComponent>()) { acc, command -> mergeComponentVisibility(acc, command.privateReferences) }
    val privateOutputs = commands.fold(mutableListOf<ZKProtectedComponent>()) { acc, command -> mergeComponentVisibility(acc, command.privateOutputs) }

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
        verifyCommandsMatchNetwork()
// TODO        TODO("TODO: Verify components visibility is aligned among different commands")
    }

    fun verify(ltx: LedgerTransaction) {
        commands.forEach { it.verify(ltx) }
    }

    fun getComponentVisibility(groupIndex: Int, componentIndex: Int): ZkpVisibility {
        return when (groupIndex) {
            ComponentGroupEnum.INPUTS_GROUP.ordinal -> ZkpVisibility.Public // References are always visible, to know State's visibility call 'getUtxoVisibility'
            ComponentGroupEnum.REFERENCES_GROUP.ordinal -> ZkpVisibility.Public // References are always visible, to know State's visibility call 'getUtxoVisibility'
            ComponentGroupEnum.OUTPUTS_GROUP.ordinal -> getVisibility(privateOutputs, componentIndex)
            else -> ZkpVisibility.Public // all other groups have visibility 'Public' by default at the moment, may change in future
        }
    }

    fun getUtxoVisibility(groupIndex: Int, componentIndex: Int): ZkpVisibility {
        return when (groupIndex) {
            ComponentGroupEnum.INPUTS_GROUP.ordinal -> getVisibility(privateInputs, componentIndex)
            ComponentGroupEnum.REFERENCES_GROUP.ordinal -> getVisibility(privateReferences, componentIndex)
            else -> error("Only Inputs and References UTXOs are allowed")
        }
    }

    private fun getVisibility(group: List<ZKProtectedComponent>, componentIndex: Int): ZkpVisibility {
        return group.find { it.index == componentIndex }?.visibility ?: ZkpVisibility.Public // Everything visible by default unless explicitly marked as Private/Mixed
    }

    private fun mergeComponentVisibility(
        acc: MutableList<ZKProtectedComponent>,
        components: List<ZKProtectedComponent>
    ): MutableList<ZKProtectedComponent> {
        components.forEach { new ->
            val existing = acc.find { it.index == new.index }
            if (existing == null)
                acc.add(new)
            else if (existing.visibility.ordinal < new.visibility.ordinal) {
                // we choose the lowest visibility requested
                acc.remove(existing)
                acc.add(new)
            }
        }
        return acc
    }
}
