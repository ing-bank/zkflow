package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.dactyloscopy.Dactyloscopist
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.transactions.toZKProverTransaction
import com.ing.zknotary.common.transactions.toZKVerifierTransaction
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import java.nio.ByteBuffer

abstract class ZKTransactionCordaService(val serviceHub: ServiceHub) : ZKTransactionService, SingletonSerializeAsToken() {

    val vtxStorage: ZKWritableVerifierTransactionStorage by lazy { serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE) as ZKWritableVerifierTransactionStorage }

    override fun prove(tx: WireTransaction): ZKVerifierTransaction {

        val ptx = tx.toZKProverTransaction(
            serviceHub,
            vtxStorage,
            componentGroupLeafDigestService = BLAKE2s256DigestService,
            nodeDigestService = PedersenDigestService
        )

        val witness = toWitness(ptx = ptx)

        // TODO
        // This construction of the circuit id is temporary and will be replaced in the subsequent work.
        // The proper id must identify circuit and its version.
        val circuitId = SecureHash.sha256(ByteBuffer.allocate(4).putInt(ptx.command.value.id).array())

        val zkService = zkServiceForTx(circuitId)
        val proof = zkService.prove(witness)

        return ptx.toZKVerifierTransaction(proof)
    }

    abstract fun zkServiceForTx(circuitId: SecureHash): ZKService

    override fun verify(stx: SignedZKVerifierTransaction) {

        val tx = stx.tx

        // Check proof
        zkServiceForTx(tx.circuitId).verify(tx.proof, calculatePublicInput(tx))

        // Check signatures
        stx.verifyRequiredSignatures()

        // Check backchain
        tx.padded.inputs().forEach { validateBackchain(it) }
        tx.padded.references().forEach { validateBackchain(it) }
    }

    private fun calculatePublicInput(tx: ZKVerifierTransaction): PublicInput {

        val paddingNonce = tx.componentGroupLeafDigestService.zeroHash
        // The hash for a non-existent output pointed to by a padded input stateRef, is
        // componentGroupLeafDigestService.hash(zeroHashNonce + FillerOutputState)
        // TODO: move this logic to somewhere in the padding config
        // FIXME: Should we get this filler from the previous transaction? Probably makes more sense
        val fillerOutput = tx.componentPaddingConfiguration.filler(ComponentGroupEnum.OUTPUTS_GROUP)
            ?: error("Expected a filler object")
        require(fillerOutput is ComponentPaddingConfiguration.Filler.TransactionState) { "Expected filler of type TransactionState" }
        val paddingHash =
            tx.componentGroupLeafDigestService.hash(paddingNonce.bytes + Dactyloscopist.identify(fillerOutput.content))

        // Prepare public input
        val inputHashes = getUtxoHashes(tx.padded.inputs(), paddingHash)
        val referenceHashes = getUtxoHashes(tx.padded.references(), paddingHash)

        return PublicInput(
            tx.id,
            inputHashes = inputHashes,
            referenceHashes = referenceHashes
        )
    }

    private fun validateBackchain(stateRef: PaddingWrapper<StateRef>) {
        when (stateRef) {
            is PaddingWrapper.Filler -> {
                // Skipping padding
            }
            is PaddingWrapper.Original -> {
                // We only recurse if we are verifying a real stateRef
                val prevVtx = vtxStorage.getTransaction(stateRef.content.txhash) ?: error("Should not happen")
                // TODO: Perhaps save this recursion until the end? Depends which order we want...
                verify(prevVtx)
            }
        }
    }

    private fun getUtxoHashes(stateRefs: List<PaddingWrapper<StateRef>>, paddingHash: SecureHash): List<SecureHash> {
        return stateRefs.map {
            collectStateRefUtxoHash(
                it,
                paddingHash = paddingHash
            )
        }
    }

    private fun collectStateRefUtxoHash(
        paddingWrapper: PaddingWrapper<StateRef>,
        paddingHash: SecureHash
    ): SecureHash {

        return if (paddingWrapper is PaddingWrapper.Original) {
            val stateRef = paddingWrapper.content
            val prevVtx = vtxStorage.getTransaction(stateRef.txhash) ?: error("Verifier tx not found for hash ${stateRef.txhash}")

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
        } else paddingHash
    }

    private fun toWitness(
        ptx: ZKProverTransaction
    ): Witness {
        loggerFor<ZKProverTransaction>().debug("Creating Witness from ProverTx")
        fun recalculateUtxoNonce(it: PaddingWrapper<StateAndRef<ContractState>>): SecureHash {
            val wtxId = vtxStorage.map.getWtxId(it.content.ref.txhash)
                ?: error("Mapping to Wtx not found for vtxId: ${it.content.ref.txhash}")

            val outputTx = serviceHub.validatedTransactions.getTransaction(wtxId)
                ?: error("Could not fetch output transaction for StateRef ${it.content.ref}")

            return ptx.componentGroupLeafDigestService.hash(
                outputTx.tx.privacySalt.bytes + ByteBuffer.allocate(8)
                    .putInt(ComponentGroupEnum.OUTPUTS_GROUP.ordinal).putInt(it.content.ref.index).array()
            )
        }

        // Because the PrivacySalt of the WireTransaction is reused to create the ProverTransactions,
        // the nonces can be calculated deterministically from WireTransaction to ZKProverTransaction.
        // This means we can recalculate the UTXO nonces for the inputs and references of the ZKProverTransaction using the
        // PrivacySalt of the WireTransaction.
        fun List<PaddingWrapper<StateAndRef<ContractState>>>.collectUtxoNonces() = mapIndexed { _, it ->
            when (it) {
                is PaddingWrapper.Filler -> {
                    // When it is a padded state, the nonce is ALWAYS a zerohash of the algo used for merkle tree leaves
                    ptx.componentGroupLeafDigestService.zeroHash
                }
                is PaddingWrapper.Original -> {
                    // When it is an original state, we look up the tx it points to and collect the nonce for the UTXO it points to.

                    // TODO: for now, we recalculate it using the PrivacySalt of the associated WireTransaction.
                    // Once we get rid of the ZKProverTransaction and use WireTransaction only, we can simply look it up
                    // from storage.
                    recalculateUtxoNonce(it)
                }
            }
        }

        // Collect the nonces for the outputs pointed to by the inputs and references.
        val inputNonces = ptx.padded.inputs().collectUtxoNonces()
        val referenceNonces = ptx.padded.references().collectUtxoNonces()

        return Witness(ptx, inputNonces = inputNonces, referenceNonces = referenceNonces)
    }
}
