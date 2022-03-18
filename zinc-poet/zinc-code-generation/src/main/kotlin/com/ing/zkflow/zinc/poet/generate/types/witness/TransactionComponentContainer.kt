package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ZKIndexedTypedElement
import com.ing.zkflow.serialization.infra.CommandDataSerializationMetadata
import com.ing.zkflow.serialization.infra.TransactionStateSerializationMetadata
import com.ing.zkflow.zinc.poet.generate.ZincTypeResolver
import com.ing.zkflow.zinc.poet.generate.types.IndexedTransactionComponent
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.digest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.parametersSecureHash
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.timeWindow
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.wrapTxComponent
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

class TransactionComponentContainer(
    private val commandMetadata: ResolvedZKCommandMetadata,
    private val standardTypes: StandardTypes,
    private val zincTypeResolver: ZincTypeResolver,
) {
    private fun whenVisibleInWitness(group: ComponentGroupEnum, maxSize: () -> Int): Int {
        return if (commandMetadata.isVisibleInWitness(group.ordinal, 0)) {
            maxSize.invoke()
        } else {
            0
        }
    }

    internal val commandGroup =
        StandardTransactionComponent(
            COMMANDS,
            wrapTxComponent(
                COMMANDS,
                zincTypeResolver.zincTypeOf(commandMetadata.commandKClass),
                CommandDataSerializationMetadata.serializer().descriptor
            ),
            whenVisibleInWitness(ComponentGroupEnum.COMMANDS_GROUP) { 1 },
            ComponentGroupEnum.COMMANDS_GROUP
        )
    internal val notaryGroup =
        StandardTransactionComponent(
            NOTARY,
            wrapTxComponent(NOTARY, standardTypes.notaryModule),
            whenVisibleInWitness(ComponentGroupEnum.NOTARY_GROUP) { 1 },
            ComponentGroupEnum.NOTARY_GROUP
        )
    internal val timeWindowGroup =
        StandardTransactionComponent(
            TIME_WINDOW,
            wrapTxComponent(TIME_WINDOW, timeWindow),
            whenVisibleInWitness(ComponentGroupEnum.TIMEWINDOW_GROUP) {
                if (commandMetadata.timeWindow) 1 else 0
            },
            ComponentGroupEnum.TIMEWINDOW_GROUP
        )
    internal val signerGroup =
        StandardTransactionComponent(
            SIGNERS,
            wrapTxComponent(SIGNERS, standardTypes.signerModule),
            // Assumption here is that either all 'numberOfSigners' signers are visible or none.
            whenVisibleInWitness(ComponentGroupEnum.SIGNERS_GROUP) { commandMetadata.numberOfSigners },
            ComponentGroupEnum.SIGNERS_GROUP
        )
    internal val parameterGroup =
        StandardTransactionComponent(
            PARAMETERS,
            wrapTxComponent(PARAMETERS, parametersSecureHash),
            whenVisibleInWitness(ComponentGroupEnum.PARAMETERS_GROUP) { 1 },
            ComponentGroupEnum.PARAMETERS_GROUP
        )

    private val privacySaltGroup = HashingMetadataTransactionComponent(PRIVACY_SALT, privacySalt, privacySalt.getSerializedTypeDef(), 1)

    private val numberOfInputs = commandMetadata.inputs.size
    private val inputNoncesGroup = HashingMetadataTransactionComponent(
        INPUT_NONCES,
        arrayOfNonceDigests(numberOfInputs),
        arrayOfSerializedNonceDigests(numberOfInputs),
        numberOfInputs
    )

    private val numberOfReferences = commandMetadata.references.size
    private val referenceNoncesGroup = HashingMetadataTransactionComponent(
        REFERENCE_NONCES,
        arrayOfNonceDigests(numberOfReferences),
        arrayOfSerializedNonceDigests(numberOfReferences),
        numberOfReferences
    )

    internal val serializedOutputGroup = OutputStateTransactionComponent(
        OUTPUTS,
        "OutputGroup",
        commandMetadata.outputs.toIndexedStateList(OUTPUTS),
        standardTypes,
    )
    internal val serializedInputUtxos = UtxosTransactionComponent(
        SERIALIZED_INPUT_UTXOS,
        "InputUtxos",
        commandMetadata.inputs.toIndexedStateList(SERIALIZED_INPUT_UTXOS),
        INPUT_NONCES,
        standardTypes,
    )
    internal val serializedReferenceUtxos = UtxosTransactionComponent(
        SERIALIZED_REFERENCE_UTXOS,
        "ReferenceUtxos",
        commandMetadata.references.toIndexedStateList(SERIALIZED_REFERENCE_UTXOS),
        REFERENCE_NONCES,
        standardTypes,
    )

    val transactionComponents: List<TransactionComponent> = listOfNotNull(
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

    private fun List<ZKIndexedTypedElement>.toIndexedStateList(groupName: String): List<IndexedTransactionComponent> = map {
        val stateType = zincTypeResolver.zincTypeOf(it.type)
        IndexedTransactionComponent(
            it.index,
            wrapTxComponent(
                "${groupName}_${stateType.id.camelToSnakeCase()}",
                standardTypes.transactionState(
                    stateType
                ),
                TransactionStateSerializationMetadata.serializer().descriptor
            )
        )
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
