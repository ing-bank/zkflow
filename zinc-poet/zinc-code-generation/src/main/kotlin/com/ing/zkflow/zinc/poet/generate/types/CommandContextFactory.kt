package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
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
    }

    companion object {
        const val COMMAND_CONTEXT = "CommandContext"
    }
}
