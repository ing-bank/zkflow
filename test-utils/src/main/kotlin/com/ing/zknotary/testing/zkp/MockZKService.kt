package com.ing.zknotary.testing.zkp

import com.ing.zknotary.common.crypto.zinc
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZKService
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.ComponentGroup
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes

@Suppress("EXPERIMENTAL_API_USAGE")
public class MockZKService(private val serviceHub: ServiceHub, private val digestService: DigestService) : ZKService {

    /**
     * This mock version simply returns the serialized witness, so that we can use it in `verify()`
     * to do all the verifications
     */
    override fun prove(witness: Witness): ByteArray {
        return witness.serialize().bytes
    }

    override fun verify(proof: ByteArray, publicInput: PublicInput) {
        val serializationFactory = SerializationFactory.defaultFactory

        // This assumes that the proof (for testing only) is simply a serialized witness.
        val witness = proof.deserialize<Witness>(serializationFactory)

        fun createComponentGroup(componentGroup: ComponentGroupEnum, components: List<ByteArray>) =
            if (components.isEmpty()) null else ComponentGroup(
                componentGroup.ordinal,
                components.map { OpaqueBytes(it) }
            )

        fun createComponentGroups(witness: Witness) = listOf(
            createComponentGroup(ComponentGroupEnum.INPUTS_GROUP, witness.inputsGroup),
            createComponentGroup(ComponentGroupEnum.OUTPUTS_GROUP, witness.outputsGroup),
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
         * Rule 2: witness.ptx.inputs[i] contents hashed with nonce should equal publicInput.prevVtxOutputHashes[i].
         * This proves that prover did not change the contents of the input states
         */
        witness.serializedInputUtxos.forEachIndexed { index, serializedInputUtxo ->
            val nonceFromWitness = witness.inputUtxoNonces.getOrElse(index) {
                error("Nonce not present in public input for input $index of tx ${wtx.id}")
            }

            val leafHashFromPublicInput = publicInput.inputHashes.getOrElse(index) {
                error("Leaf hash not present in public input for input $index of tx ${wtx.id}")
            }

            val calculatedLeafHashFromWitness =
                DigestService.zinc.componentHash(nonceFromWitness, OpaqueBytes(serializedInputUtxo))

            if (leafHashFromPublicInput != calculatedLeafHashFromWitness) error(
                "Calculated leaf hash ($calculatedLeafHashFromWitness} for input $index of tx ${wtx.id} does " +
                    "not match the leaf hash from the public input ($leafHashFromPublicInput)."
            )
        }

        /*
         * Rule 3: witness.ptx.references[i] contents hashed with nonce should equal publicreference.prevVtxOutputHashes[i].
         * This proves that prover did not change the contents of the reference states
         */
        witness.serializedReferenceUtxos.forEachIndexed { index, serializedReferenceUtxo ->
            val nonceFromWitness = witness.referenceUtxoNonces.getOrElse(index) {
                error("Nonce not present in public reference for reference $index of tx ${wtx.id}")
            }

            val leafHashFromPublicreference = publicInput.referenceHashes.getOrElse(index) {
                error("Leaf hash not present in public reference for reference $index of tx ${wtx.id}")
            }

            val calculatedLeafHashFromWitness =
                DigestService.zinc.componentHash(nonceFromWitness, OpaqueBytes(serializedReferenceUtxo))

            if (leafHashFromPublicreference != calculatedLeafHashFromWitness) error(
                "Calculated leaf hash ($calculatedLeafHashFromWitness} for reference $index of tx ${wtx.id} does " +
                    "not match the leaf hash from the public input ($leafHashFromPublicreference)."
            )
        }

        /*
         * Rule 4: The contract rules should verify
         */
        verifyContract(witness, wtx)
    }

    private fun verifyContract(witness: Witness, wtx: WireTransaction) {
        val inputs = witness.serializedInputUtxos.mapIndexed { index, bytes ->
            bytes.deserialize<TransactionState<ContractState>>()
            StateAndRef<ContractState>(
                bytes.deserialize(),
                wtx.inputs[index]
            )
        }
        val references = witness.serializedReferenceUtxos.mapIndexed { index, bytes ->
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
