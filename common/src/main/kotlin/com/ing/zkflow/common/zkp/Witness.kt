package com.ing.zkflow.common.zkp

import com.ing.zinc.naming.camelToZincSnakeCase
import com.ing.zkflow.common.transactions.UtxoInfo
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.serialization.zincTypeName
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.PrivacySalt
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

@CordaSerializable
@Suppress("LongParameterList")
class Witness(
    /**
     * These groups match the groups defined in [ComponentGroupEnum].
     * They contain the serialized form of [TraversableTransaction] components.
     */

    /**
     * Serialized Map<ContractState.name, TransactionState<ContractState>>
     */
    val outputsGroup: List<WitnessField>,

    /**
     * Serialized List<CommandData>
     */
    val commandsGroup: List<ByteArray>,

    /**
     * Serialized List<SecureHash>
     */
    val attachmentsGroup: List<ByteArray>,

    /**
     * Serialized List<Party>
     */
    val notaryGroup: List<ByteArray>,

    /**
     * List<TimeWindow>
     */
    val timeWindowGroup: List<ByteArray>,

    /**
     * Serialized List<List<PublicKey>>
     */
    val signersGroup: List<ByteArray>,

    /**
     * Serialized List<SecureHash>
     */
    val parametersGroup: List<ByteArray>,

    /**
     * Note that this is *not* serialized or sized, since [PrivacySalt] is already a list of bytes that is fixed size.
     */
    val privacySalt: PrivacySalt,

    /**
     * These are the input StateRefs in serialized form
     */
    val inputsGroup: List<ByteArray>,

    /**
     * The serialized UTXOs (TransactionState<T: ContractState>) pointed to by the serialized stateRefs of the inputsGroup parameter.
     *
     * They should be indexed identically to the inputsGroup parameter.
     */
    val serializedInputUtxos: List<WitnessField>,

    /**
     * The serialized UTXOs (TransactionState<T: ContractState>) pointed to by the serialized stateRefs of the referencesGroup parameter.
     *
     * They should be indexed identically to the referencesGroup parameter.
     */
    val serializedReferenceUtxos: List<WitnessField>,

    /**
     * The nonce hashes ([SecureHash]) for the UTXOs pointed to by the serialized stateRefs of the inputsGroup parameter.
     * Note that these are *not* serialized or sized, since [SecureHash] is already a list of bytes that is fixed size.
     *
     * They should be indexed identically to the inputsGroup parameter.
     */
    val inputUtxoNonces: List<SecureHash>,

    /**
     * The nonce hashes ([SecureHash]) for the UTXOs pointed to by the serialized stateRefs of the referencesGroup parameter.
     * Note that these are *not* serialized or sized, since [SecureHash] is already a list of bytes that is fixed size.
     *
     * They should be indexed indentically to the inputsGroup parameter.
     */
    val referenceUtxoNonces: List<SecureHash>
) {
    fun size(filter: (Byte) -> Boolean = { true }): Int {
        val paddedByteListSize = { it: List<ByteArray> -> it.fold(0) { acc, bytes -> acc + bytes.filter(filter).size } }
        val paddedHashListSize = { it: List<SecureHash> -> it.fold(0) { acc, hash -> acc + hash.bytes.filter(filter).size } }
        val paddedPairListSize = { it: List<WitnessField> -> it.fold(0) { acc, field -> acc + field.serializedData.filter(filter).size } }

        return outputsGroup.run(paddedPairListSize) +
            commandsGroup.run(paddedByteListSize) +
            attachmentsGroup.run(paddedByteListSize) +
            notaryGroup.run(paddedByteListSize) +
            timeWindowGroup.run(paddedByteListSize) +
            signersGroup.run(paddedByteListSize) +
            parametersGroup.run(paddedByteListSize) +
            privacySalt.size +
            inputsGroup.run(paddedByteListSize) +
            serializedInputUtxos.run(paddedPairListSize) +
            serializedReferenceUtxos.run(paddedPairListSize) +
            inputUtxoNonces.run(paddedHashListSize) +
            referenceUtxoNonces.run(paddedHashListSize)
    }

    companion object {
        fun fromWireTransaction(
            wtx: WireTransaction,
            inputUtxoInfos: List<UtxoInfo>,
            referenceUtxoInfos: List<UtxoInfo>,
            metadata: ResolvedZKCommandMetadata
        ): Witness {

            // Reorder utxos according to be consistent with the order in the WireTransaction.
            val orderedInputUtxoInfos = wtx.inputs.map { inputRef ->
                inputUtxoInfos.find { it.stateRef == inputRef } ?: error("No UtxoInfo provided for input ref $inputRef")
            }

            val orderedReferenceUtxoInfos = wtx.references.map { referenceRef ->
                referenceUtxoInfos.find { it.stateRef == referenceRef } ?: error("No UtxoInfo provided for reference ref $referenceRef")
            }

            return Witness(
                outputsGroup = wtx.serializedComponentBytesForOutputGroup(metadata),
                commandsGroup = wtx.serializedComponentBytesFor(ComponentGroupEnum.COMMANDS_GROUP, metadata),
                attachmentsGroup = wtx.serializedComponentBytesFor(ComponentGroupEnum.ATTACHMENTS_GROUP, metadata),
                notaryGroup = wtx.serializedComponentBytesFor(ComponentGroupEnum.NOTARY_GROUP, metadata),
                timeWindowGroup = wtx.serializedComponentBytesFor(ComponentGroupEnum.TIMEWINDOW_GROUP, metadata),
                signersGroup = wtx.serializedComponentBytesFor(ComponentGroupEnum.SIGNERS_GROUP, metadata),
                parametersGroup = wtx.serializedComponentBytesFor(ComponentGroupEnum.PARAMETERS_GROUP, metadata),

                privacySalt = wtx.privacySalt,

                inputsGroup = wtx.serializedComponentBytesFor(ComponentGroupEnum.INPUTS_GROUP, metadata),
                serializedInputUtxos = orderedInputUtxoInfos.serializedBytesForUTXO(ComponentGroupEnum.INPUTS_GROUP, metadata),
                serializedReferenceUtxos = orderedReferenceUtxoInfos.serializedBytesForUTXO(ComponentGroupEnum.REFERENCES_GROUP, metadata),
                inputUtxoNonces = orderedInputUtxoInfos.map { it.nonce },
                referenceUtxoNonces = orderedReferenceUtxoInfos.map { it.nonce }
            )
        }

        private fun TraversableTransaction.serializedComponentBytesForOutputGroup(
            metadata: ResolvedZKCommandMetadata
        ): List<WitnessField> {
            val outputsGroupIndex = ComponentGroupEnum.OUTPUTS_GROUP.ordinal

            val zincTypes = outputs.filterIndexed { index, _ -> metadata.isVisibleInWitness(outputsGroupIndex, index) }
                .map { it.data::class.zincTypeName.camelToZincSnakeCase() }

            val serializedStateBytes: List<WitnessField>? =
                componentGroups.singleOrNull { it.groupIndex == outputsGroupIndex }?.components
                    ?.mapIndexedNotNull { index, outputBytes ->
                        if (metadata.isVisibleInWitness(outputsGroupIndex, index)) {
                            WitnessField("${zincTypes[index]}_$index", outputBytes.copyBytes())
                        } else {
                            null
                        }
                    }

            return serializedStateBytes ?: emptyList()
        }

        /**
         * Input and reference UTXOs can contain different states. Therefor their serialized bytes grouped by their state names.
         */
        private fun List<UtxoInfo>.serializedBytesForUTXO(
            groupEnum: ComponentGroupEnum,
            metadata: ResolvedZKCommandMetadata
        ): List<WitnessField> {
            require(groupEnum == ComponentGroupEnum.INPUTS_GROUP || groupEnum == ComponentGroupEnum.REFERENCES_GROUP) { "Only input and reference groups are valid for UTXO serialization." }

            return this.mapIndexedNotNull { index, utxo ->
                if (metadata.isVisibleInWitness(groupEnum.ordinal, index)) {
                    WitnessField("${utxo.stateClass.zincTypeName.camelToZincSnakeCase()}_$index", utxo.serializedContents)
                } else {
                    null
                }
            }
        }
    }
}

@CordaSerializable
@Suppress("ArrayInDataClass") // Only used for serialization to Zinc
data class WitnessField(
    val name: String,
    val serializedData: ByteArray,
)
