package com.ing.zkflow.testing.zkp

import com.ing.zkflow.common.serialization.zinc.json.WitnessSerializer
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZKService
import kotlinx.serialization.json.Json
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.ComponentGroup
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.loggerFor

@Suppress("EXPERIMENTAL_API_USAGE", "DuplicatedCode")
public class MockZKService(private val serviceHub: ServiceHub, private val digestService: DigestService) : ZKService {
    private val log = loggerFor<MockZKService>()

    /**
     * This mock version simply returns the serialized witness, so that we can use it in `verify()`
     * to do all the verifications
     */
    override fun prove(witness: Witness): ByteArray {
        log.debug("Witness size: ${witness.size()}")
        log.debug("Padded Witness size: ${witness.size { it == 0.toByte() }}") // Assumes BFL zero-byte padding
        val witnessJson = Json.encodeToString(WitnessSerializer, witness)
        log.trace("Witness JSON: $witnessJson")

        return witness.serialize().bytes
    }

    override fun verify(proof: ByteArray, publicInput: PublicInput) {
        // This assumes that the proof (for testing only) is simply a serialized witness.
        val witness = proof.deserialize<Witness>()

        fun createComponentGroup(componentGroup: ComponentGroupEnum, components: List<ByteArray>) =
            if (components.isEmpty()) null else ComponentGroup(
                componentGroup.ordinal,
                components.map { OpaqueBytes(it) }
            )

        fun createComponentGroupForMultiState(componentGroup: ComponentGroupEnum, components: Map<String, List<ByteArray>>) =
            if (components.isEmpty()) null else ComponentGroup(
                componentGroup.ordinal,
                components.flatMap { it.value }.map { OpaqueBytes(it) }
            )

        fun createComponentGroups(witness: Witness) = listOf(
            createComponentGroup(ComponentGroupEnum.INPUTS_GROUP, witness.inputsGroup),
            createComponentGroupForMultiState(ComponentGroupEnum.OUTPUTS_GROUP, witness.outputsGroup),
            createComponentGroup(ComponentGroupEnum.COMMANDS_GROUP, witness.commandsGroup),
            createComponentGroup(ComponentGroupEnum.ATTACHMENTS_GROUP, witness.attachmentsGroup),
            createComponentGroup(ComponentGroupEnum.NOTARY_GROUP, witness.notaryGroup),
            createComponentGroup(ComponentGroupEnum.TIMEWINDOW_GROUP, witness.timeWindowGroup),
            createComponentGroup(ComponentGroupEnum.SIGNERS_GROUP, witness.signersGroup),
            createComponentGroup(ComponentGroupEnum.REFERENCES_GROUP, witness.referencesGroup),
            createComponentGroup(ComponentGroupEnum.PARAMETERS_GROUP, witness.parametersGroup)
        ).mapNotNull { it }

        val wtx = WireTransaction(
            createComponentGroups(witness),
            witness.privacySalt,
            digestService
        )

        /*
         * Rule 1: The recalculated Merkle root should match the one from the instance vtx.
         *
         * In this case on the Corda side, a ZKProverTransaction id is lazily recalculated always. This means it is
         * always a direct representation of the ptx contents so we don't have to do a recalculation.
         * On the Zinc side, we will need explicit recalculation based on the witness transaction components.
         *
         * Here, we simply compare the witness.ptx.id with the instance.currentVtxId.
         * This proves that the inputs whose contents have been verified to be unchanged, are also part of the vtx
         * being verified.
         */
        if (publicInput.transactionId != wtx.id) error(
            "The calculated Merkle root from the witness (${wtx.id}) does not match " +
                "the expected transaction id from the public input (${publicInput.transactionId})."
        )

        /*
         * Rule 2: witness input and reference contents hashed with their nonce should equal the matching hash from publicInput.
         * This proves that prover did not change the contents of the input states
         */
        verifyUtxoContents(witness.serializedInputUtxos, witness.inputUtxoNonces, publicInput.inputHashes)
        verifyUtxoContents(witness.serializedReferenceUtxos, witness.referenceUtxoNonces, publicInput.referenceHashes)

        /*
         * Rule 3: The contract rules should verify
         */
        verifyContract(witness, wtx)
    }

    private fun verifyUtxoContents(
        serializedUtxos: Map<String, List<ByteArray>>,
        utxoNonces: List<SecureHash>,
        expectedUtxoHashes: List<SecureHash>
    ) {
        serializedUtxos.flatMap { e -> e.value }
            .forEachIndexed { index, serializedReferenceUtxo ->
                val nonceFromWitness = utxoNonces.getOrElse(index) {
                    error("Nonce not present in public input for reference $index")
                }

                val leafHashFromPublicreference = expectedUtxoHashes.getOrElse(index) {
                    error("Leaf hash not present in public input for reference $index")
                }

                val calculatedLeafHashFromWitness =
                    digestService.componentHash(nonceFromWitness, OpaqueBytes(serializedReferenceUtxo))

                if (leafHashFromPublicreference != calculatedLeafHashFromWitness) error(
                    "Calculated leaf hash ($calculatedLeafHashFromWitness} for reference $index does " +
                        "not match the leaf hash from the public input ($leafHashFromPublicreference)."
                )
            }
    }

    private fun verifyContract(witness: Witness, wtx: WireTransaction) {
        val inputs = witness.serializedInputUtxos.flatMap { it.value }
            .mapIndexed { index, bytes ->
                bytes.deserialize<TransactionState<ContractState>>()
                StateAndRef<ContractState>(
                    bytes.deserialize(),
                    wtx.inputs[index]
                )
            }

        val references = witness.serializedReferenceUtxos.flatMap { it.value }.mapIndexed { index, bytes ->
            bytes.deserialize<TransactionState<ContractState>>()
            StateAndRef<ContractState>(
                bytes.deserialize(),
                wtx.references[index]
            )
        }

        val ltx = LedgerTransaction.createForSandbox(
            inputs = inputs,
            outputs = wtx.outputs, // witness.outputsGroup.map { it.deserialize() },
            commands = wtx.commands.map {
                CommandWithParties(
                    value = it.value,
                    signingParties = it.signers.mapNotNull { serviceHub.identityService.partyFromKey(it) },
                    signers = it.signers
                )
            },
            attachments = wtx.attachments.map { serviceHub.attachments.openAttachment(it) ?: throw AttachmentResolutionException(it) },
            id = wtx.id,
            notary = wtx.notary,
            timeWindow = wtx.timeWindow,
            privacySalt = wtx.privacySalt,
            networkParameters = wtx.networkParametersHash?.let { serviceHub.networkParametersService.lookup(it) }
                ?: throw TransactionResolutionException.UnknownParametersException(wtx.id, wtx.networkParametersHash!!),
            references = references,
            wtx.digestService
        )
        ltx.verify()
    }
}
