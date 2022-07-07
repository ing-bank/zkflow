package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransactionWithoutProofs
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.ZKTransactionResolutionException
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

abstract class AbstractZKTransactionService(val serviceHub: ServiceHub) : ZKTransactionService,
    SingletonSerializeAsToken() {

    override val vtxStorage: ZKVerifierTransactionStorage by lazy {
        serviceHub.getCordaServiceFromConfig(
            ServiceNames.ZK_VERIFIER_TX_STORAGE
        )
    }

    override fun prove(
        wtx: WireTransaction
    ): ZKVerifierTransaction {
        val zkTransactionMetadata = wtx.zkTransactionMetadata(serviceHub)
        val proofs = mutableMapOf<String, ByteArray>()

        zkTransactionMetadata.commands.forEach { command ->
            val commandName = command.commandKClass.qualifiedName!!
            // TODO: Should we add a check here that this command actually requires a proof? What if all its  outputs are 'public', then we wouldn't need a proof?
            if (!proofs.containsKey(commandName)) {
                val witness = Witness.fromWireTransaction(
                    wtx,
                    serviceHub.collectUtxoInfos(wtx.inputs),
                    serviceHub.collectUtxoInfos(wtx.references),
                    command
                )
                proofs[commandName] = zkServiceForCommandMetadata(command).proveTimed(witness)
            }
        }

        return ZKVerifierTransaction.fromWireTransaction(wtx, proofs)
    }

    abstract override fun zkServiceForCommandMetadata(metadata: ResolvedZKCommandMetadata): ZKService

    override fun verify(wtx: WireTransaction): ZKVerifierTransactionWithoutProofs {
        val zkTransactionMetadata = wtx.zkTransactionMetadata(serviceHub)
        val vtx =
            ZKVerifierTransactionWithoutProofs.fromWireTransaction(wtx) // create vtx without proofs just to be able to build witness and public input

        // Check transaction structure first, so we fail fast
        vtx.verifyMerkleTree()

        // Verify private components by running ZVM smart contract code per Command
        zkTransactionMetadata.commands.forEach { command ->
            val witness = Witness.fromWireTransaction(
                wtx, serviceHub.collectUtxoInfos(wtx.inputs), serviceHub.collectUtxoInfos(wtx.references),
                command
            )
            zkServiceForCommandMetadata(command).run(witness, calculatePublicInput(vtx, command))
        }

        return vtx
    }

    override fun verify(vtx: ZKVerifierTransaction) {
        // Check transaction structure first, so we fail fast
        vtx.verifyMerkleTree()

        // Verify the ZKPs for all ZKCommandDatas in this transaction
        verifyProofs(vtx)
    }

    private fun verifyProofs(vtx: ZKVerifierTransaction) {
        // Check there is a proof for each ZKCommand
        vtx.zkTransactionMetadata(serviceHub).commands.forEach { zkCommand ->
            // For ZK Commands we check proofs
            val proof = vtx.proofs[zkCommand.commandKClass.qualifiedName]
            require(proof != null) { "Proof is missing for command ${zkCommand.commandSimpleName}" }
            zkServiceForCommandMetadata(zkCommand).verifyTimed(proof, calculatePublicInput(vtx, zkCommand))
        }
    }

    override fun validateBackchain(tx: TraversableTransaction) {
        (tx.inputs + tx.references).groupBy { it.txhash }.keys.forEach {
            vtxStorage.getTransaction(it) ?: throw ZKTransactionResolutionException(it)
        }
    }

    open fun calculatePublicInput(tx: ZKVerifierTransaction, commandMetadata: ResolvedZKCommandMetadata): PublicInput {
        // Fetch the UTXO hashes from the svtx's pointed to by the inputs and references.
        // This confirms that we have a validated backchain stored for them.
        val privateInputHashes = tx.inputs.filterIndexed { index, _ ->
            commandMetadata.isVisibleInWitness(ComponentGroupEnum.INPUTS_GROUP.ordinal, index)
        }.let { getUtxoHashes(it) }

        val privateReferenceHashes = tx.references.filterIndexed { index, _ ->
            commandMetadata.isVisibleInWitness(ComponentGroupEnum.REFERENCES_GROUP.ordinal, index)
        }.let { privateStateRefs -> getUtxoHashes(privateStateRefs) }

        val privateOutputHashes = tx.outputHashes().filterIndexed { index, _ ->
            commandMetadata.isVisibleInWitness(ComponentGroupEnum.OUTPUTS_GROUP.ordinal, index)
        }

        return PublicInput(
            outputComponentHashes = privateOutputHashes,
            attachmentComponentHashes = tx.visibleInWitnessComponentHashes(commandMetadata, ComponentGroupEnum.ATTACHMENTS_GROUP),
            commandComponentHashes = tx.visibleInWitnessComponentHashes(commandMetadata, ComponentGroupEnum.COMMANDS_GROUP),
            notaryComponentHashes = tx.visibleInWitnessComponentHashes(commandMetadata, ComponentGroupEnum.NOTARY_GROUP),
            parametersComponentHashes = tx.visibleInWitnessComponentHashes(commandMetadata, ComponentGroupEnum.PARAMETERS_GROUP),
            timeWindowComponentHashes = tx.visibleInWitnessComponentHashes(commandMetadata, ComponentGroupEnum.TIMEWINDOW_GROUP),
            signersComponentHashes = tx.visibleInWitnessComponentHashes(commandMetadata, ComponentGroupEnum.SIGNERS_GROUP),

            inputUtxoHashes = privateInputHashes,
            referenceUtxoHashes = privateReferenceHashes,
            inputStateRefs = tx.serializedComponentBytesFor(ComponentGroupEnum.INPUTS_GROUP, commandMetadata)

        )
    }

    private fun getUtxoHashes(stateRefs: List<StateRef>): List<SecureHash> {
        return stateRefs.map { stateRef ->
            val prevVtx = vtxStorage.getTransaction(stateRef.txhash) ?: throw ZKTransactionResolutionException(stateRef.txhash)

            /*
             * To be able to verify that the stateRefs that are used in the transaction are correct, and unchanged from
             * when they were outputs in the previous tx, the verifier needs both the Merkle hash for each output and
             * the nonce that was used to create those Merkle hashes.
             *
             * These values will be used as part of the instance when verifying the proof.
             */
            prevVtx.tx.outputHashes()[stateRef.index]
            /*
             * Now the verifier calls currentVtx.proof.verify(currentVtx.id, prevVtx.outputHashes).
             *
             * Inside the circuit, the prover proves:
             * - witnessTx.stateRefs[i] contents hashed with nonce from witness should equal instance.moveTxstateRefHashesFromPrevTx[i].
             *   This proves that prover did not change the contents of the state
             * - Recalculates witnessTx merkleRoot based on all components from the witness, including witnessTx.stateRefs.
             * - witnessTx.merkleRoot == instance.moveTx.id. This proves the witnessTx is the same as the ZKVerifierTransaction
             *   that the verifier is trying to verify. It also proves that the stateRefs consumed are indeed part of the
             *   transaction identified by the instance.
             */
        }
    }
}
