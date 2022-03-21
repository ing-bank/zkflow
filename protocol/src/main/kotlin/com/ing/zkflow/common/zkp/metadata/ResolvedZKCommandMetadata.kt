package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Crypto
import net.corda.core.transactions.LedgerTransaction
import java.io.File
import java.time.Duration
import kotlin.reflect.KClass

data class ResolvedZKCircuit(
    val commandKClass: KClass<out ZKCommandData>,
    var name: String,
    /**
     * Unless provided, this will be calculated to be `<gradle module>/src/main/zinc/<transaction.name>/commands/<command.name>`
     * This is where the circuit elements for this command can be found
     */
    val buildFolder: File,
    val buildTimeout: Duration,
    val setupTimeout: Duration,
    val provingTimeout: Duration,
    val verificationTimeout: Duration
)

data class ResolvedZKCommandMetadata(
    /**
     * Information on the circuit and related artifacts to be used.
     */
    val circuit: ResolvedZKCircuit,
    val commandKClass: KClass<out ZKCommandData>,
    val numberOfSigners: Int,
    val inputs: List<ZKReference>,
    val references: List<ZKReference>,
    val outputs: List<ZKProtectedComponent>,
    val command: Boolean,
    val notary: Boolean,
    val timeWindow: Boolean,
) {
    val networkParameters = true
    val commandSimpleName: String by lazy { commandKClass.simpleName ?: error("Command classes must be a named class") }
    val contractClassNames: List<ContractClassName>
        get() {
            val stateTypes = (inputs + outputs + references).map { it.type }.distinct()
            return stateTypes.map {
                requireNotNull(it.requiredContractClassName) {
                    "Unable to infer Contract class name because state class $it is not annotated with " +
                        "@BelongsToContract, and does not have an enclosing class which implements Contract."
                }
            }
        }

    val privateInputTypeGroups = countTypes(inputs)
    val privateReferenceTypeGroups = countTypes(references)
    val privateOutputTypeGroups = countTypes(outputs)

    private fun countTypes(components: List<ZKIndexedTypedElement>): Map<KClass<out ContractState>, Int> {
        val result = mutableMapOf<KClass<out ContractState>, Int>()

        components.forEach {
            result[it.type] = result.getOrDefault(it.type, 0) + 1
        }
        return result
    }

    /**
     * Verify that the LedgerTransaction matches the expected structure defined in this metadata
     */
    fun verify(ltx: LedgerTransaction) {
        try {
            // TODO verifyCommandsAndSigners(ltx.commands.map { Command(it.value, it.signers) })
            verifyOutputs(ltx.outputs)
            verifyInputs(ltx.inputs)
            verifyReferences(ltx.references)
            // TODO verifyNotary(ltx.notary)
            // TODO verifyParameters(ltx)
        } catch (e: IllegalArgumentException) {
            throw IllegalTransactionStructureException(e)
        }
    }

    /**
     * Verify that the ZKTransactionBuilder matches the expected structure defined in this metadata
     * TODO: See if we can make sure this is always called, perhaps by calling it just before calling contract.verify
     * Alternatively, we can force users to extend ZKContract, which will do this for them and then delegate to normal verify function
     */
    fun verify(txb: ZKTransactionBuilder) {
        try {
            verifyCommandsAndSigners(txb)
            verifyOutputs(txb.outputStates())
            verifyInputs(txb.inputsWithTransactionState)
            verifyReferences(txb.referencesWithTransactionState)
            // TODO verifyNotary(txb)
            // TODO verifyParameters(txb)
        } catch (e: IllegalArgumentException) {
            throw IllegalTransactionStructureException(e)
        }
    }

    private fun verifyCommandsAndSigners(txb: ZKTransactionBuilder) {
        val unverifiedCommands = txb.commands()
        val actualCommand = unverifiedCommands.find {
            it.value::class == commandKClass
        } ?: error("Transaction does not include a Command with type $commandKClass")

        require(actualCommand.signers.size == numberOfSigners) {
            "Expected '$numberOfSigners signers for command $actualCommand, but found '${actualCommand.signers.size}'."
        }

        actualCommand.signers.forEachIndexed { signerIndex, key ->
            val actualScheme = Crypto.findSignatureScheme(key)
            require(actualScheme == txb.zkNetworkParameters.participantSignatureScheme) {
                "Signer $signerIndex of command '${actualCommand.value::class}' should use signature scheme: '${txb.zkNetworkParameters.participantSignatureScheme.schemeCodeName}, but found '${actualScheme.schemeCodeName}'"
            }
        }
    }

    class IllegalTransactionStructureException(cause: Throwable) :
        IllegalArgumentException("Transaction does not match expected structure: ${cause.message}", cause)

    private fun verifyInputs(inputs: List<StateAndRef<ContractState>>) {
        matchTypes(
            componentName = "input",
            expectedTypes = this.inputs.map { it.type },
            actualTypes = inputs.filterIndexed { index, _ -> this.inputs.any { it.index == index } }.map { it.state.data::class }
        )
    }

    @JvmName("verifyReferenceTransactionStates")
    private fun verifyReferences(references: List<TransactionState<ContractState>>) {
        matchTypes(
            componentName = "reference",
            expectedTypes = this.references.map { it.type },
            actualTypes = references.filterIndexed { index, _ -> this.references.any { it.index == index } }.map { it.data::class }
        )
    }

    private fun verifyReferences(references: List<StateAndRef<ContractState>>) = verifyReferences(references.map { it.state })

    private fun verifyOutputs(outputs: List<TransactionState<*>>) {
        matchTypes(
            "output",
            expectedTypes = this.outputs.map { it.type },
            actualTypes = outputs.filterIndexed { index, _ -> this.outputs.any { it.index == index } }.map { it.data::class }
        )
    }

    private fun matchTypes(componentName: String, expectedTypes: List<KClass<*>>, actualTypes: List<KClass<*>>) {
        require(expectedTypes.size == actualTypes.size) { "Expected types size (${expectedTypes.size}) has different length than actual size (${actualTypes.size})" }
        expectedTypes.forEachIndexed { index, expected ->
            val actual = actualTypes.getOrElse(index) { error("Expected to find $componentName $expected at index $index, nothing found") }
            require(actual == expected) { "Unexpected $componentName order. Expected '$expected' at index $index, but found '$actual'" }
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
            ComponentGroupEnum.COMMANDS_GROUP.ordinal -> command
            ComponentGroupEnum.NOTARY_GROUP.ordinal -> notary
            ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal -> timeWindow
            ComponentGroupEnum.SIGNERS_GROUP.ordinal -> numberOfSigners > 0
            else -> false // other groups are not part of the witness for now, may change in the future.
        }
    }
}
