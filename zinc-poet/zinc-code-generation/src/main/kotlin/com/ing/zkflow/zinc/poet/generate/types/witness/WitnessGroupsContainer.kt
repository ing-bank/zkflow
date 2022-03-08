package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ZKIndexedTypedElement
import com.ing.zkflow.zinc.poet.generate.ZincTypeResolver
import com.ing.zkflow.zinc.poet.generate.types.IndexedState
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.digest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.parametersSecureHash
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.timeWindow
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.COMMANDS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUT_NONCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.NOTARY
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.OUTPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PARAMETERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PRIVACY_SALT
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.REFERENCE_NONCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_INPUT_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_REFERENCE_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SIGNERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.TIME_WINDOW
import net.corda.core.contracts.ComponentGroupEnum

class WitnessGroupsContainer(
    commandMetadata: ResolvedZKCommandMetadata,
    standardTypes: StandardTypes,
    private val zincTypeResolver: ZincTypeResolver,
) {
    private val inputs = commandMetadata.inputs.toIndexedStateList()
    private val outputs = commandMetadata.outputs.toIndexedStateList()
    private val references = commandMetadata.references.toIndexedStateList()

    private val commandGroup =
        StandardComponentWitnessGroup(COMMANDS, zincTypeResolver.zincTypeOf(commandMetadata.commandKClass), 1, ComponentGroupEnum.COMMANDS_GROUP)
    private val notaryGroup =
        StandardComponentWitnessGroup(NOTARY, standardTypes.notaryModule, 1, ComponentGroupEnum.NOTARY_GROUP)
    private val timeWindowGroup =
        StandardComponentWitnessGroup(
            TIME_WINDOW,
            timeWindow,
            if (commandMetadata.timeWindow) 1 else 0,
            ComponentGroupEnum.TIMEWINDOW_GROUP
        )
    private val signerGroup =
        StandardComponentWitnessGroup(
            SIGNERS,
            standardTypes.signerModule,
            commandMetadata.numberOfSigners,
            ComponentGroupEnum.SIGNERS_GROUP
        )
    private val parameterGroup =
        StandardComponentWitnessGroup(PARAMETERS, parametersSecureHash, 1, ComponentGroupEnum.PARAMETERS_GROUP)

    private val privacySaltGroup = HashingMetadataWitnessGroup(PRIVACY_SALT, privacySalt, privacySalt.getSerializedTypeDef(), 1)

    private val numberOfInputs = commandMetadata.inputs.size
    private val inputNoncesGroup = HashingMetadataWitnessGroup(
        INPUT_NONCES,
        arrayOfNonceDigests(numberOfInputs),
        arrayOfSerializedNonceDigests(numberOfInputs),
        numberOfInputs
    )

    private val numberOfReferences = commandMetadata.references.size
    private val referenceNoncesGroup = HashingMetadataWitnessGroup(
        REFERENCE_NONCES,
        arrayOfNonceDigests(numberOfReferences),
        arrayOfSerializedNonceDigests(numberOfReferences),
        numberOfReferences
    )

    internal val serializedOutputGroup = OutputStateWitnessGroup(
        OUTPUTS,
        "OutputGroup",
        outputs,
        standardTypes,
    )
    internal val serializedInputUtxos = UtxosWitnessGroup(
        SERIALIZED_INPUT_UTXOS,
        "InputUtxos",
        inputs,
        INPUT_NONCES,
        standardTypes,
    )
    internal val serializedReferenceUtxos = UtxosWitnessGroup(
        SERIALIZED_REFERENCE_UTXOS,
        "ReferenceUtxos",
        references,
        REFERENCE_NONCES,
        standardTypes,
    )

    val witnessGroups: List<WitnessGroup> = listOfNotNull(
        serializedOutputGroup,
        commandGroup,
        notaryGroup,
        timeWindowGroup,
        signerGroup,
        parameterGroup,
        privacySaltGroup,
        inputNoncesGroup,
        referenceNoncesGroup,
        serializedInputUtxos,
        serializedReferenceUtxos
    ).filter { it.isPresent }

    private fun List<ZKIndexedTypedElement>.toIndexedStateList(): List<IndexedState> = map {
        IndexedState(it.index, zincTypeResolver.zincTypeOf(it.type))
    }

    companion object {
        private fun arrayOfNonceDigests(count: Int) = array {
            capacity = count
            elementType = digest
        }

        private fun arrayOfSerializedNonceDigests(count: Int) = zincArray {
            size = "$count"
            elementType = digest.getSerializedTypeDef()
        }
    }
}
