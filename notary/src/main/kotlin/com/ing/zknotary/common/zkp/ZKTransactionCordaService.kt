package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.dactyloscopy.Dactyloscopist
import com.ing.zknotary.common.transactions.SignedZKProverTransaction
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.transactions.toWitness
import com.ing.zknotary.common.transactions.toZKProverTransaction
import com.ing.zknotary.common.transactions.toZKVerifierTransaction
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKWritableProverTransactionStorage
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.WireTransaction

/**
 * In fact it is an abstract class that incapsulates operations that will be same for all implementations,
 * but we can make it a class because ancestors of this class are also forced to inherit SerializeAsToken implementation
 */
abstract class ZKTransactionCordaService(val serviceHub: ServiceHub) : ZKTransactionService, SingletonSerializeAsToken() {

    val ptxStorage: ZKWritableProverTransactionStorage by lazy { serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_PROVER_TX_STORAGE) as ZKWritableProverTransactionStorage }
    val vtxStorage: ZKWritableVerifierTransactionStorage by lazy { serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE) as ZKWritableVerifierTransactionStorage }

    override fun prove(tx: WireTransaction): ZKVerifierTransaction = prove(tx, true)

    fun prove(tx: WireTransaction, persistProverTx: Boolean): ZKVerifierTransaction {

        val zkService = zkServiceForTx(tx.commands)

        val ptx = tx.toZKProverTransaction(
            serviceHub,
            vtxStorage,
            componentGroupLeafDigestService = BLAKE2s256DigestService,
            nodeDigestService = PedersenDigestService
        )

        val witness = ptx.toWitness(ptxStorage)

        val proof = zkService.prove(witness)

        val vtx = ptx.toZKVerifierTransaction(proof)

        // TODO this should be either removed if we decide not to store PTX or moved somewhere otherwise
        if (persistProverTx) {
            ptxStorage.map.put(tx, ptx)
            // TODO we don't really need sigs for PTX and also nowhere to get them from here in sane manner
            //  should either store non-signed PTX or don't store it at all - to discuss
            ptxStorage.addTransaction(SignedZKProverTransaction(ptx, emptyList()))
        }

        return vtx
    }

    abstract fun zkServiceForTx(commands: List<Command<*>>): ZKService

    // TODO obe of zkServiceForTx should go, probably this one
    //  (or both if we use single ZKService and it will figure out what circuit to use on its own)
    abstract fun zkServiceForTx(circuitId: SecureHash): ZKService

    override fun verify(stx: SignedZKVerifierTransaction) {

        val tx = stx.tx

        // FIXME: until we have support for different circuits per tx/command, even issuance txs will need to have
        // nonces set in the witness for inputs and references.
        // if (currentVtx.inputs.isEmpty() && currentVtx.references.isEmpty()) {
        //     println("   $indent No inputs and references")
        // }

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
             * Now the verifier calls currentVtx.proof.verify(currentVtx.id, prevVtx.outputHashes, prevVtx.outputNonces).
             *
             * Inside the circuit, the prover proves:
             * - witnessTx.stateRefs[i] contents hashed with nonce should equal instance.moveTxstateRefHashesFromPrevTx[i].
             *   This proves that prover did not change the contents of the state
             * - Recalculates witnessTx merkleRoot based on all components from the witness, including witnessTx.stateRefs.
             * - witnessTx.merkleRoot == instance.moveTx.id. This proves the witnessTx is the same as the ZKVerifierTransaction
             *   that the verifier is trying to verify. It also proves that the stateRefs consumed are indeed part of the
             *   transaction identified by the instance.
             */
        } else paddingHash
    }
}
