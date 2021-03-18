package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serialization.WitnessSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

/**
 * The witness, which is what we serialize for Zinc, contains the following items:
 *
 * Already serialized & sized componentgroups, e.g. groups of bytearrays of the WireTransaction.
 * Already serialized & sized TransactionState<T: ContractState> class instances for all UTXOs (outputs of the previous transaction) pointed to by the inputs and reference StateRefs serialized inside the inputs and references component groups of the WireTransaction.
 * The nonces bytes for the UTXOs pointed to by the input and reference StateRefs. (Unsized because hashes are serialized and sized by nature? Or should this be serialized & sized also?)
 *
 * Then in Zinc, the following happens respectively:
 *
 * We recalculate the Merkle root using the sized & serialized bytearrays of the componentgroups as is.
 * Next, they are deserialized into the expected transaction structs used for contract rule validation. Rule violation fails proof generation.
 * Finally the Merkle root is 'compared' with the expected Merkle root from the public input, which would fail proof verification if not matching.
 * and 3. The sized & serialized UTXOs are hashed together with their nonces to get the Merkle tree hashes for the UTXOs. These are 'compared' with the UTXO hashes from the public input. This proves that the contract rules have been applied on inputs and references that are unchanged since they were created in the preceding transactions. Next, the UTXOs are deserialized into the expected TransactionState<T> structs and used, together with the transaction struct from 1. for contract rule verification.
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
    val inputsGroup: List<ByteArray>,
    val outputsGroup: List<ByteArray>,
    val commandsGroup: List<ByteArray>,
    val attachmentsGroup: List<ByteArray>,
    val notaryGroup: List<ByteArray>,
    val timeWindowGroup: List<ByteArray>,
    val signersGroup: List<ByteArray>,
    val referencesGroup: List<ByteArray>,
    val parametersGroup: List<ByteArray>,

    val privacySalt: @Contextual PrivacySalt,

    val inputStates: List<@Contextual StateAndRef<ContractState>>,
    val referenceStates: List<@Contextual StateAndRef<ContractState>>,
    val inputNonces: List<@Contextual SecureHash>,
    val referenceNonces: List<@Contextual SecureHash>,

    /**
     * This is only here so that we can use it in MockZKService to reconstruct the WireTransaction.
     * It should not be passed on to Zinc
     */
    val digestService: DigestService
) {
    companion object {
        fun fromWireTransaction(
            tx: WireTransaction,
            inputStates: List<StateAndRef<ContractState>>,
            referenceStates: List<StateAndRef<ContractState>>,
            inputNonces: List<SecureHash>,
            referenceNonces: List<SecureHash>
        ): Witness {
            return Witness(
                inputsGroup = tx.serializedComponenteBytesFor(ComponentGroupEnum.INPUTS_GROUP),
                outputsGroup = tx.serializedComponenteBytesFor(ComponentGroupEnum.OUTPUTS_GROUP),
                commandsGroup = tx.serializedComponenteBytesFor(ComponentGroupEnum.COMMANDS_GROUP),
                attachmentsGroup = tx.serializedComponenteBytesFor(ComponentGroupEnum.ATTACHMENTS_GROUP),
                notaryGroup = tx.serializedComponenteBytesFor(ComponentGroupEnum.NOTARY_GROUP),
                timeWindowGroup = tx.serializedComponenteBytesFor(ComponentGroupEnum.TIMEWINDOW_GROUP),
                signersGroup = tx.serializedComponenteBytesFor(ComponentGroupEnum.SIGNERS_GROUP),
                referencesGroup = tx.serializedComponenteBytesFor(ComponentGroupEnum.REFERENCES_GROUP),
                parametersGroup = tx.serializedComponenteBytesFor(ComponentGroupEnum.PARAMETERS_GROUP),

                privacySalt = tx.privacySalt,

                inputStates = inputStates,
                referenceStates = referenceStates,
                inputNonces = inputNonces,
                referenceNonces = referenceNonces,

                digestService = tx.digestService
            )
        }

        private fun TraversableTransaction.serializedComponenteBytesFor(groupEnum: ComponentGroupEnum): List<ByteArray> {
            return componentGroups.singleOrNull { it.groupIndex == groupEnum.ordinal }?.components?.map { it.copyBytes() }
                ?: emptyList()
        }
    }
}
