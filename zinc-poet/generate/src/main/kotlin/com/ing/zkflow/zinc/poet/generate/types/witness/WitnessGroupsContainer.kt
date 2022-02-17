package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ZKTypedElement
import com.ing.zkflow.zinc.poet.generate.ZincTypeResolver
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.digest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.secureHash
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
import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

class WitnessGroupsContainer(
    commandMetadata: ResolvedZKCommandMetadata,
    standardTypes: StandardTypes,
    private val zincTypeResolver: ZincTypeResolver,
) {
    private val inputs = commandMetadata.privateInputs.toBflModuleMap()
    private val outputs = commandMetadata.privateOutputs.toBflModuleMap()
    private val references = commandMetadata.privateReferences.toBflModuleMap()

    private val commandGroup =
        StandardComponentWitnessGroup(COMMANDS, BflPrimitive.U32, 1, ComponentGroupEnum.COMMANDS_GROUP)
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
        StandardComponentWitnessGroup(PARAMETERS, secureHash, 1, ComponentGroupEnum.PARAMETERS_GROUP)

    private val privacySaltGroup = HashingMetadataWitnessGroup(PRIVACY_SALT, privacySalt, privacySalt.getSerializedTypeDef(), 1)

    private val numberOfInputs = commandMetadata.privateInputs.size
    private val inputNoncesGroup = HashingMetadataWitnessGroup(
        INPUT_NONCES,
        arrayOfNonceDigests(numberOfInputs),
        arrayOfSerializedNonceDigests(numberOfInputs),
        numberOfInputs
    )

    private val numberOfReferences = commandMetadata.privateReferences.size
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

    private fun List<ZKTypedElement>.toBflModuleMap(): Map<BflModule, Int> {
        return this.fold<ZKTypedElement, MutableMap<KClass<out ContractState>, Int>>(mutableMapOf()) { acc, zkReference: ZKTypedElement ->
            acc[zkReference.type] = acc[zkReference.type]?.let { it + 1 } ?: 1
            acc
        }.mapKeys {
            zincTypeResolver.zincTypeOf(it.key)
        }
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
