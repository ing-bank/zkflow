package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.secureHash
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.timeWindow
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.NOTARY
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.OUTPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PARAMETERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.REFERENCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SIGNERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.TIME_WINDOW
import com.ing.zkflow.zinc.poet.generate.types.witness.WitnessGroupsContainer

class CommandContextFactory(
    private val standardTypes: StandardTypes,
) {
    fun createCommandContext(
        commandMetadata: ResolvedZKCommandMetadata,
        witnessGroupsContainer: WitnessGroupsContainer,
    ): BflStruct = struct {
        name = COMMAND_CONTEXT
        if (commandMetadata.privateInputs.isNotEmpty()) {
            field { name = INPUTS; type = witnessGroupsContainer.serializedInputUtxos.deserializedGroup }
        }
        if (commandMetadata.privateOutputs.isNotEmpty()) {
            field { name = OUTPUTS; type = witnessGroupsContainer.serializedOutputGroup.deserializedGroup }
        }
        if (commandMetadata.privateReferences.isNotEmpty()) {
            field { name = REFERENCES; type = witnessGroupsContainer.serializedReferenceUtxos.deserializedGroup }
        }
        field { name = NOTARY; type = standardTypes.notaryModule }
        if (commandMetadata.timeWindow) {
            field { name = TIME_WINDOW; type = timeWindow }
        }
        field { name = PARAMETERS; type = secureHash }
        field { name = SIGNERS; type = standardTypes.signerList(commandMetadata) }
        isDeserializable = false
    }

    companion object {
        const val COMMAND_CONTEXT = "CommandContext"
    }
}
