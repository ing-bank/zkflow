package com.ing.zkflow.common.zkp

import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zkflow.common.serialization.zinc.generation.zincTypeName
import com.ing.zkflow.common.transactions.UtxoInfo
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.PrivacySalt
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

/**
 * The witness, which is what we serialize for Zinc, contains the following items:
 *
 * - Already serialized & sized componentgroups, e.g. groups of bytearrays of the WireTransaction.
 * - Already serialized & sized TransactionState<T: ContractState> class instances for all UTXOs (outputs of the previous transaction) pointed to by the inputs and reference StateRefs serialized inside the inputs and references component groups of the WireTransaction.
 * - The nonces bytes for the UTXOs pointed to by the input and reference StateRefs. (TODO: currently unsized because hashes are serialized and sized by nature. Or should this be serialized & sized also?)
 *
 * Then in Zinc, the following happens respectively:
 *
 * - We recalculate the Merkle root using the sized & serialized bytearrays of the componentgroups as is. The Merkle root is compared with the expected Merkle root from the public input, which would fail proof verification if not matching.
 * - The sized & serialized UTXOs from the witness are hashed together with the respective nonces from the witness to get the Merkle tree hashes
 *   for the UTXOs pointed to by the staterefs from the inputs and references component groups from the witness.
 *   These are compared with the UTXO hashes from the public input. If they match, this proves that the contract rules have been applied on the
 *   contents of inputs and references that are unchanged since they were created in the preceding transactions.
 *   Next, the UTXOs are deserialized into the expected TransactionState<T> structs and used, together with the transaction struct from
 *   from step 1 for contract rule verification.
 * - Next, the components are deserialized into the expected transaction structs used for contract rule validation. Rule violation fails proof generation.
 *
 * Please validate these assumptions:
 *
 * The only data type sent to Zinc via JSON are byte arrays?
 * On the Kotlin side, serialization and deserialization sizes and unsizes respectively, invisibly for the user.
 * On the Zinc side, we never serialize. On deserialization, unsizing does not happen.
 */

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

                serializedInputUtxos = orderedInputUtxoInfos.serializedBytesForUTXO(ComponentGroupEnum.INPUTS_GROUP, metadata),
                serializedReferenceUtxos = orderedReferenceUtxoInfos.serializedBytesForUTXO(ComponentGroupEnum.REFERENCES_GROUP, metadata),
                inputUtxoNonces = orderedInputUtxoInfos.map { it.nonce },
                referenceUtxoNonces = orderedReferenceUtxoInfos.map { it.nonce }
            )
        }

        private fun TraversableTransaction.serializedComponentBytesFor(
            groupEnum: ComponentGroupEnum,
            metadata: ResolvedZKCommandMetadata
        ): List<ByteArray> {
            return componentGroups.singleOrNull { it.groupIndex == groupEnum.ordinal }?.components
                ?.filterIndexed { index, _ -> metadata.isVisibleInWitness(groupEnum.ordinal, index) }
                ?.map { it.copyBytes() } ?: emptyList()
        }

        private fun TraversableTransaction.serializedComponentBytesForOutputGroup(
            metadata: ResolvedZKCommandMetadata
        ): List<WitnessField> {
            val outputsGroupIndex = ComponentGroupEnum.OUTPUTS_GROUP.ordinal

            val zincTypes = outputs.filterIndexed { index, _ -> metadata.isVisibleInWitness(outputsGroupIndex, index) }
                .map { it.data::class.zincTypeName.camelToSnakeCase() }

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
                    WitnessField("${utxo.stateClass.zincTypeName.camelToSnakeCase()}_$index", utxo.serializedContents)
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
