package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serialization.json.corda.WitnessSerializer
import com.ing.zknotary.common.transactions.UtxoInfo
import com.ing.zknotary.gradle.zinc.template.TemplateParameters.Companion.camelToSnakeCase
import kotlinx.serialization.Serializable
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
@Serializable(with = WitnessSerializer::class)
@Suppress("LongParameterList")
class Witness(
    /**
     * These groups match the groups defined in [ComponentGroupEnum].
     * They contain the serialized form of [TraversableTransaction] components.
     */

    /**
     * Serialized List<StateRef>
     */
    val inputsGroup: List<ByteArray>,

    /**
     * Serialized Map<ContractState.name, TransactionState<ContractState>>
     */
    val outputsGroup: Map<String, List<ByteArray>>,

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
     * Serialized List<StateRef>
     */
    val referencesGroup: List<ByteArray>,

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
     * They should be indexed indentically to the inputsGroup parameter.
     */
    val serializedInputUtxos: Map<String, List<ByteArray>>,

    /**
     * The serialized UTXOs (TransactionState<T: ContractState>) pointed to by the serialized stateRefs of the referencesGroup parameter.
     *
     * They should be indexed indentically to the referencesGroup parameter.
     */
    val serializedReferenceUtxos: Map<String, List<ByteArray>>,

    /**
     * The nonce hashes ([SecureHash]) for the UTXOs pointed to by the serialized stateRefs of the inputsGroup parameter.
     * Note that these are *not* serialized or sized, since [SecureHash] is already a list of bytes that is fixed size.
     *
     * They should be indexed indentically to the inputsGroup parameter.
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
    companion object {
        fun fromWireTransaction(
            wtx: WireTransaction,
            inputUtxoInfos: List<UtxoInfo>,
            referenceUtxoInfos: List<UtxoInfo>,
        ): Witness {
            return Witness(
                inputsGroup = wtx.serializedComponenteBytesFor(ComponentGroupEnum.INPUTS_GROUP),
                outputsGroup = wtx.serializedComponentBytesForOutputGroup(ComponentGroupEnum.OUTPUTS_GROUP),
                commandsGroup = wtx.serializedComponenteBytesFor(ComponentGroupEnum.COMMANDS_GROUP),
                attachmentsGroup = wtx.serializedComponenteBytesFor(ComponentGroupEnum.ATTACHMENTS_GROUP),
                notaryGroup = wtx.serializedComponenteBytesFor(ComponentGroupEnum.NOTARY_GROUP),
                timeWindowGroup = wtx.serializedComponenteBytesFor(ComponentGroupEnum.TIMEWINDOW_GROUP),
                signersGroup = wtx.serializedComponenteBytesFor(ComponentGroupEnum.SIGNERS_GROUP),
                referencesGroup = wtx.serializedComponenteBytesFor(ComponentGroupEnum.REFERENCES_GROUP),
                parametersGroup = wtx.serializedComponenteBytesFor(ComponentGroupEnum.PARAMETERS_GROUP),

                privacySalt = wtx.privacySalt,

                serializedInputUtxos = inputUtxoInfos.serializedBytesForUTXO("input"),
                serializedReferenceUtxos = referenceUtxoInfos.serializedBytesForUTXO("reference"),
                inputUtxoNonces = inputUtxoInfos.map { it.nonce },
                referenceUtxoNonces = referenceUtxoInfos.map { it.nonce }
            )
        }

        private fun TraversableTransaction.serializedComponenteBytesFor(groupEnum: ComponentGroupEnum): List<ByteArray> {
            return componentGroups.singleOrNull { it.groupIndex == groupEnum.ordinal }?.components?.map { it.copyBytes() }
                ?: emptyList()
        }

        private fun TraversableTransaction.serializedComponentBytesForOutputGroup(groupEnum: ComponentGroupEnum): Map<String, List<ByteArray>> {
            val stateNames = outputs.map {
                "output${it.data.javaClass.simpleName}".camelToSnakeCase()
            }
            val serializedStateBytes =
                componentGroups.singleOrNull { it.groupIndex == groupEnum.ordinal }?.components?.mapIndexed { index, output -> stateNames[index] to output.copyBytes() }

            return serializedStateBytes?.groupBy { it.first }?.map { it.key to it.value.map { bytes -> bytes.second } }
                ?.toMap() ?: emptyMap()
        }

        private fun List<UtxoInfo>.serializedBytesForUTXO(componentName: String): Map<String, List<ByteArray>> {
            return this.map { (componentName + it.stateName).camelToSnakeCase() to it.serializedContents }
                .groupBy { it.first }.map { it.key to it.value.map { bytes -> bytes.second } }
                .toMap()
        }
    }
}
