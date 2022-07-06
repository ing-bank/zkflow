package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.parametersSecureHash
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.timeWindow
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUTS
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
         * - validateStatesAgainstContract() // THis may be irrelevant: if they don't belong to this circuit, Zinc will not understand them.
         *
         * TODO: Confirm that users will not need to check contract attachments for private transaction components:
         * Currently, users will have a verification key for each command in their CorDapp loaded by the node, also for all historical
         * commands. This is ensured, since command classes can never change in ZKFlow. If they do, a new one must be introduced, which
         * also results in a new circuit for it, including keys.
         * This means that as long as the CorDapps the user has deployed on the node are trusted, they have all verification keys
         * for the commands that they know. This does not need to be retrieved from the transaction attachments.
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
         */
        addFunction(generateCheckNoNotaryChangeMethod(transactionComponents))
    }

    // TransactionVerifierServiceInternal.checkNoNotaryChange()
    private fun generateCheckNoNotaryChangeMethod(transactionComponents: TransactionComponentContainer) = ZincMethod.zincMethod {
        name = "check_no_notary_change"
        returnType = ZincPrimitive.Unit
        body = with(transactionComponents) {
            if (notaryGroup.isPresent && (serializedInputUtxos.isPresent || serializedReferenceUtxos.isPresent)) {
                if (serializedOutputGroup.isPresent) {
                    serializedOutputGroup.deserializedGroup.fields.fold("") { acc, output ->
                        acc + "assert!(self.outputs.${output.name}.notary.equals(self.notary), \"Found unexpected notary change in transaction. Check that output notaries match transaction notary.\");\n"
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
