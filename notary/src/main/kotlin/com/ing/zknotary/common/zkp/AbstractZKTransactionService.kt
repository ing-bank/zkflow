package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.dactyloscopy.Dactyloscopist
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.node.services.ZKVerifierTransactionStorage
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash

/**
 * In fact it is an abstract class that incapsulates operations that will be same for all implementations,
 * but we can make it a class because ancestors of this class are also forced to inherit SerializeAsToken implementation
 */
interface AbstractZKTransactionService : ZKTransactionService {

    val zkStorage: ZKVerifierTransactionStorage

    override fun verify(tx: ZKVerifierTransaction) {

        // verify the tx graph for each input and collect nonces and hashes for current tx verification
        val inputHashes = mutableListOf<SecureHash>()
        val referenceHashes = mutableListOf<SecureHash>()

        // FIXME: until we have support for different circuits per tx/command, even issuance txs will need to have
        // nonces set in the witness for inputs and references.
        // if (currentVtx.inputs.isEmpty() && currentVtx.references.isEmpty()) {
        //     println("   $indent No inputs and references")
        // }

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

        fun List<PaddingWrapper<StateRef>>.collectUtxoHashesInto(hashes: MutableList<SecureHash>) =
            forEachIndexed { index, it ->
                val collectedHash = collectStateRefUtxoHash(
                    it,
                    paddingHash = paddingHash
                )

                hashes.add(index, collectedHash)
            }

        tx.padded.inputs().collectUtxoHashesInto(inputHashes)
        tx.padded.references().collectUtxoHashesInto(referenceHashes)

        val calculatedPublicInput = PublicInput(
            tx.id,
            inputHashes = inputHashes,
            referenceHashes = referenceHashes
        )

        verify(tx.proof, calculatedPublicInput)
    }

    private fun collectStateRefUtxoHash(
        paddingWrapper: PaddingWrapper<StateRef>,
        paddingHash: SecureHash
    ): SecureHash {

        return if (paddingWrapper is PaddingWrapper.Original) {
            val stateRef = paddingWrapper.content
            val prevVtx = zkStorage.getTransaction(stateRef.txhash) ?: error("Should not happen")

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
