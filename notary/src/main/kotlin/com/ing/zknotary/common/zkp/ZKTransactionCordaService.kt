package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.transactions.collectUtxoInfos
import com.ing.zknotary.common.transactions.zkCommandData
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKTransactionResolutionException
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import kotlinx.serialization.ExperimentalSerializationApi
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

abstract class ZKTransactionCordaService(val serviceHub: ServiceHub) : ZKTransactionService,
    SingletonSerializeAsToken() {

    private val vtxStorage: ZKWritableVerifierTransactionStorage by lazy {
        serviceHub.getCordaServiceFromConfig(
            ServiceNames.ZK_VERIFIER_TX_STORAGE
        ) as ZKWritableVerifierTransactionStorage
    }

    @ExperimentalSerializationApi
    override fun prove(
        wtx: WireTransaction
    ): ZKVerifierTransaction {

        val witness = Witness.fromWireTransaction(
            wtx,
            serviceHub.collectUtxoInfos(wtx.inputs),
            serviceHub.collectUtxoInfos(wtx.references)
        )

        val zkService = zkServiceForCommand(wtx.zkCommandData())
        val proof = zkService.prove(witness)

        return ZKVerifierTransaction.fromWireTransaction(wtx, proof)
    }

    abstract fun zkServiceForCommand(command: ZKCommandData): ZKService

    override fun verify(svtx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean) {
        val tx = svtx.tx

        // Check proof
        zkServiceForCommand(tx.zkCommandData).verify(tx.proof, calculatePublicInput(tx))

        // Check signatures
        if (checkSufficientSignatures) {
            svtx.verifyRequiredSignatures()
        } else {
            svtx.checkSignaturesAreValid()
        }

        // Check transaction structure
        svtx.tx.verify()

        // Check backchain
        validateBackchain(svtx.tx)
    }

    override fun validateBackchain(tx: TraversableTransaction) {
        // TODO: group these by txhash, perhaps some are from the same tx, so this saves validations. Then call `validateBackchain(stateRef.txhash)`
        tx.inputs.forEach { validateBackchain(it) }
        tx.references.forEach { validateBackchain(it) }
    }

    private fun validateBackchain(stateRef: StateRef) {
        vtxStorage.getTransaction(stateRef.txhash) ?: throw ZKTransactionResolutionException(stateRef.txhash)
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
