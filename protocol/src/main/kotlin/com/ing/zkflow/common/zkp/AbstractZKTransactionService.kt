package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.ZKTransactionResolutionException
import com.ing.zkflow.node.services.ZKWritableVerifierTransactionStorage
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

    private val vtxStorage: ZKWritableVerifierTransactionStorage by lazy {
        serviceHub.getCordaServiceFromConfig(
            ServiceNames.ZK_VERIFIER_TX_STORAGE
        ) as ZKWritableVerifierTransactionStorage
    }

    override fun prove(
        wtx: WireTransaction
    ): ZKVerifierTransaction {

        val zkTransactionMetadata = wtx.zkTransactionMetadata()
        val proofs = mutableMapOf<String, ByteArray>()

        zkTransactionMetadata.commands.forEach { command ->
            val commandName = command.commandKClass.qualifiedName!!
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

    override fun verify(svtx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean) {
        val vtx = svtx.tx

        // Check transaction structure first, so we fail fast
        vtx.verify()

        // Check there is a proof for each ZKCommand
        vtx.commands.forEach { command ->
            if (command.value is ZKCommandData) require(vtx.proofs.containsKey(command.value::class.qualifiedName)) { "Proof is missing for command ${command.value}" }
        }

        // Check proofs
        vtx.proofs.forEach { (commandClassName, proof) ->
            val command = vtx.commands.single { it.value::class.qualifiedName == commandClassName }.value as ZKCommandData
            zkServiceForCommandMetadata(command.metadata).verifyTimed(proof, calculatePublicInput(vtx, command.metadata))
        }

        // Check signatures
        if (checkSufficientSignatures) {
            svtx.verifyRequiredSignatures()
        } else {
            svtx.checkSignaturesAreValid()
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
        val privateInputIndices = commandMetadata.privateInputs.map { it.index }
        val privateInputHashes = getUtxoHashes(tx.inputs).filterIndexed { index, _ -> privateInputIndices.contains(index) }

        val privateReferenceIndices = commandMetadata.privateReferences.map { it.index }
        val privateReferenceHashes = getUtxoHashes(tx.references).filterIndexed { index, _ -> privateReferenceIndices.contains(index) }

        // Fetch output component hashes for private outputs of the command
        val privateOutputIndices = commandMetadata.privateOutputs.map { it.index }
        val privateOutputHashes = tx.outputHashes.filterIndexed { index, _ ->
            privateOutputIndices.contains(index)
        }

        return PublicInput(
            inputComponentHashes = emptyList(), // StateRefs are always public
            outputComponentHashes = privateOutputHashes,
            referenceComponentHashes = emptyList(), // StateRefs are always public
            attachmentComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal].orEmpty(),
            commandComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.COMMANDS_GROUP.ordinal].orEmpty(),
            notaryComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.NOTARY_GROUP.ordinal].orEmpty(),
            parametersComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.PARAMETERS_GROUP.ordinal].orEmpty(),
            timeWindowComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal].orEmpty(),
            signersComponentHashes = tx.privateComponentHashes[ComponentGroupEnum.SIGNERS_GROUP.ordinal].orEmpty(),

            inputUtxoHashes = privateInputHashes,
            referenceUtxoHashes = privateReferenceHashes
        )
    }

    private fun getUtxoHashes(stateRefs: List<StateRef>): List<SecureHash> {
        return stateRefs.map { stateRef ->
            val prevVtx = vtxStorage.getTransaction(stateRef.txhash)
                ?: throw ZKTransactionResolutionException(stateRef.txhash)

            /*
             * To be able to verify that the stateRefs that are used in the transaction are correct, and unchanged from
             * when they were outputs in the previous tx, the verifier needs both the Merkle hash for each output and
             * the nonce that was used to create those Merkle hashes.
             *
             * These values will be used as part of the instance when verifying the proof.
             */
            prevVtx.tx.outputHashes[stateRef.index]
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
