package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.parametersSecureHash
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.timeWindow
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUT_STATEREFS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.NOTARY
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.OUTPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PARAMETERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.REFERENCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SIGNERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.TIME_WINDOW
import com.ing.zkflow.zinc.poet.generate.types.witness.TransactionComponentContainer

class CommandContextFactory(
    private val standardTypes: StandardTypes,
) {
    fun createCommandContext(
        commandMetadata: ResolvedZKCommandMetadata,
        transactionComponents: TransactionComponentContainer,
    ): BflStruct = struct {
        name = COMMAND_CONTEXT
        if (transactionComponents.inputStateRefsGroup.isPresent) {
            field {
                name = INPUT_STATEREFS; type = array {
                    capacity = commandMetadata.inputs.size
                    elementType = standardTypes.stateRef
                }
            } // standardTypes.stateRefList(commandMetadata) }
        }
        if (transactionComponents.serializedInputUtxos.isPresent) {
            field { name = INPUTS; type = transactionComponents.serializedInputUtxos.deserializedGroup }
        }
        if (transactionComponents.serializedOutputGroup.isPresent) {
            field { name = OUTPUTS; type = transactionComponents.serializedOutputGroup.deserializedGroup }
        }
        if (transactionComponents.serializedReferenceUtxos.isPresent) {
            field { name = REFERENCES; type = transactionComponents.serializedReferenceUtxos.deserializedGroup }
        }
        if (transactionComponents.notaryGroup.isPresent) {
            field { name = NOTARY; type = standardTypes.notaryModule }
        }
        if (transactionComponents.timeWindowGroup.isPresent) {
            field { name = TIME_WINDOW; type = timeWindow }
        }
        if (transactionComponents.parameterGroup.isPresent) {
            field { name = PARAMETERS; type = parametersSecureHash }
        }
        if (transactionComponents.signerGroup.isPresent) {
            field { name = SIGNERS; type = standardTypes.signerList(commandMetadata) }
        }
        isDeserializable = false

        /**
         * All relevant checks on (private) data from [net.corda.core.internal.Verifier.verify]:
         * - checkNoNotaryChange()
         * - checkEncumbrancesValid()
         *
         * Currently, users will have a verification key for each command in their CorDapp loaded by the node, also for all historical
         * commands. This is ensured, since command classes can never change in ZKFlow. If they do, a new one must be introduced, which
         * also results in a new circuit for it, including keys.
         * This means that as long as the CorDapps the user has deployed on the node are trusted, they have all verification keys
         * for the commands that they know. They do not need to be retrieved from the transaction attachments. This means
         * that it is not required to check that a trusted attachment with a contract is present in the tx for a state. So no checks on
         * contract attachments are done for private states.
         * For the verification of the public parts of a ZKFlow transaction, all normal checks will be done by Corda as usual, including these.
         * - val contractAttachmentsByContract = getUniqueContractAttachmentsByContract()
         * - verifyConstraints(contractAttachmentsByContract)
         * - verifyConstraintsValidity(contractAttachmentsByContract)
         *
         * Some checks are not necessary to implement in the circuit: these components are always visible and checks are already done
         * on them outside the circuit when [com.ing.zkflow.common.transactions.verification.ZKTransactionVerifierService.validatePublicComponents]
         * constructs and validates a LedgerTransaction from the visible components of the transaction:
         * - ltx.checkSupportedHashType():
         * - com.ing.zkflow.common.transactions.verification.ZKTransactionVerifierService.validatePublicComponents
         * - checkTransactionWithTimeWindowIsNotarised()
         * - checkNotaryWhitelisted(ltx)
         *
         * Finally, validateStatesAgainstContract() is irrelevant: if states don't belong to this circuit, it will not be able deserialize
         * the witness and will always fail.
         */
        addFunction(generateCheckNoNotaryChangeMethod(transactionComponents))
        addFunction(generateCheckEncumbrancesValidMethod(transactionComponents))
    }

    /**
     * [net.corda.core.internal.Verifier.checkEncumbrancesValid]
     */
    private fun generateCheckEncumbrancesValidMethod(transactionComponents: TransactionComponentContainer) = ZincMethod.zincMethod {
        name = "check_encumbrances_valid"
        returnType = ZincPrimitive.Unit
        body = with(transactionComponents) {
            val inputsCheck = if (serializedInputUtxos.isPresent) {
                serializedInputUtxos.deserializedGroup.fields.foldIndexed("") { encumberedInputIndex, inputsCheck, inputField ->
                    // val encumbranceStateExists = ltx.inputs.any {
                    //     it.ref.txhash == ref.txhash && it.ref.index == state.encumbrance
                    // }
                    inputsCheck + """
                       if self.inputs.${inputField.name}.encumbrance.has_value {
                           let encumbranceStateExists: bool = ${serializedInputUtxos.deserializedGroup.fields.foldIndexed(listOf<String>()) { otherInputindex, encumbranceExistsCheck, _ ->
                        if (otherInputindex == encumberedInputIndex) {
                            encumbranceExistsCheck
                        } else {
                            encumbranceExistsCheck + (
                                "(self.input_stateref_components[$otherInputindex].txhash.equals(self.input_stateref_components[$encumberedInputIndex].txhash) && " +
                                    "self.input_stateref_components[$otherInputindex].index == self.inputs.${inputField.name}.encumbrance.value)"
                                )
                        }
                    }.joinToString(" || \n").ifEmpty { "false; // There is only one input and it is encumbered, so no other encumbering states exist in the transaction. " } };
                           assert!(encumbranceStateExists, "Missing required encumbrance in inputs for ${inputField.name}.");
                       };
                    """.trimIndent() + "\n"
                }
            } else {
                "// No inputs present"
            }

            /**
             * MISSING CHECK: add the outputs check
             * // Check that in the outputs,
             * // a) an encumbered state does not refer to itself as the encumbrance
             * // b) the number of outputs can contain the encumbrance
             * // c) the bi-directionality (full cycle) property is satisfied
             * // d) encumbered output states are assigned to the same notary.
             * val statesAndEncumbrance = ltx.outputs
             *     .withIndex()
             *     .filter { it.value.encumbrance != null }
             *     .map { Pair(it.index, it.value.encumbrance!!) }
             *
             * if (statesAndEncumbrance.isNotEmpty()) {
             *     checkBidirectionalOutputEncumbrances(statesAndEncumbrance)
             *     checkNotariesOutputEncumbrance(statesAndEncumbrance)
             * }
             */
            val outputsCheck = ""

            """
               $inputsCheck 
               $outputsCheck
            """.trimIndent()
        }
    }

    /**
     * [net.corda.core.internal.Verifier.checkNoNotaryChange]
     */
    private fun generateCheckNoNotaryChangeMethod(transactionComponents: TransactionComponentContainer) = ZincMethod.zincMethod {
        name = "check_no_notary_change"
        returnType = ZincPrimitive.Unit
        body = with(transactionComponents) {
            if (notaryGroup.isPresent && (serializedInputUtxos.isPresent || serializedReferenceUtxos.isPresent)) {
                if (serializedOutputGroup.isPresent) {
                    serializedOutputGroup.deserializedGroup.fields.fold("") { acc, outputField ->
                        acc + "assert!(self.outputs.${outputField.name}.notary.equals(self.notary), \"Found unexpected notary change in transaction. Check that output notaries match transaction notary.\");\n"
                    }
                } else {
                    ""
                }
            } else {
                "// $NOTARY not present in transaction"
            }
        }
    }

    companion object {
        const val COMMAND_CONTEXT = "CommandContext"
    }
}
