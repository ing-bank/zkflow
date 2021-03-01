package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.transactions.toWitness
import com.ing.zknotary.common.transactions.toZKVerifierTransaction
import com.ing.zknotary.common.transactions.zkCommandData
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.WireTransaction

abstract class ZKTransactionCordaService(val serviceHub: ServiceHub) : ZKTransactionService, SingletonSerializeAsToken() {

    private val vtxStorage: ZKWritableVerifierTransactionStorage by lazy { serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE) as ZKWritableVerifierTransactionStorage }

    override fun prove(wtx: WireTransaction, inputs: List<StateAndRef<ContractState>>): ZKVerifierTransaction {

        val witness = wtx.toWitness(inputs = inputs)

        // TODO
        // This construction of the circuit id is temporary and will be replaced in the subsequent work.
        // The proper id must identify circuit and its version.
        val circuitId = wtx.zkCommandData().circuitId()

        val zkService = zkServiceForTx(circuitId)
        val proof = zkService.prove(witness)

        return wtx.toZKVerifierTransaction(proof)
    }

    abstract fun zkServiceForTx(circuitId: SecureHash): ZKService

    override fun verify(stx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean) {

        val tx = stx.tx

        // Check proof
        zkServiceForTx(tx.circuitId).verify(tx.proof, calculatePublicInput(tx))

        // Check signatures
        if (checkSufficientSignatures) {
            stx.verifyRequiredSignatures()
        } else {
            stx.checkSignaturesAreValid()
        }

        // Check backchain
        tx.inputs.forEach { validateBackchain(it) }
        tx.references.forEach { validateBackchain(it) }
    }

    private fun calculatePublicInput(tx: ZKVerifierTransaction): PublicInput {

        // Prepare public input
        val inputHashes = getUtxoHashes(tx.inputs)
        val referenceHashes = getUtxoHashes(tx.references)

        return PublicInput(
                tx.id,
                inputHashes = inputHashes,
                referenceHashes = referenceHashes
        )
    }

    private fun validateBackchain(stateRef: StateRef) {
        val prevVtx = vtxStorage.getTransaction(stateRef.txhash) ?: error("Should not happen")
        // TODO: Perhaps save this recursion until the end? Depends which order we want...
        verify(prevVtx, true)
    }

    private fun getUtxoHashes(stateRefs: List<StateRef>): List<SecureHash> {
        return stateRefs.map { stateRef ->
            val prevVtx = vtxStorage.getTransaction(stateRef.txhash)
                    ?: error("Verifier tx not found for hash ${stateRef.txhash}")

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
