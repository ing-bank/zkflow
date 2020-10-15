package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.dactyloscopy.fingerprint
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.ZincZKTransactionService
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.WireTransaction
import net.corda.testing.node.ledger
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class VerificationService(private val proverService: ProverService) {
    private val ledgerServices = proverService.ledgerServices
    private val zkStorage = proverService.zkStorage
    private val zkTransactionServices = proverService.zkTransactionServices

    fun verify(tx: WireTransaction) {
        println("\nVerifying chain leading to: ${tx.id.toString().take(8)}")

        val verificationTime = measureTime {
            ledgerServices.ledger {
                println("Starting recursive verification:")
                val vtx = zkStorage.zkVerifierTransactionFor(tx) ?: error("No corresponding Verifier Tx found for Wire Tx ${tx.id}")
                verify(vtx)
            }
        }

        println("Overall verification time: $verificationTime")
    }

    /**
     * Verify each tx recursively ("walking back the chain")
     */
    private fun verify(currentVtx: ZKVerifierTransaction, level: Int = 0) {
        val indent = " ".repeat(level * 6) + "|-"
        println("$indent Verifying TX at level $level: ${currentVtx.id.toString().take(8)}")

        // verify the tx graph for each input and collect nonces and hashes for current tx verification
        val inputHashes = mutableListOf<SecureHash>()
        val referenceHashes = mutableListOf<SecureHash>()

        // FIXME: until we have support for different circuits per tx/command, even issuance txs will need to have
        // nonces set in the witness for inputs and references.
        // if (currentVtx.inputs.isEmpty() && currentVtx.references.isEmpty()) {
        //     println("   $indent No inputs and references")
        // }

        val paddingNonce = currentVtx.componentGroupLeafDigestService.zeroHash
        // The hash for a non-existent output pointed to by a padded input stateRef, is
        // componentGroupLeafDigestService.hash(zeroHashNonce + FillerOutputState)
        // TODO: move this logic to somewhere in the padding config
        // FIXME: Should we get this filler from the previous transaction? Probably makes more sense
        val fillerOutput = currentVtx.componentPaddingConfiguration.filler(ComponentGroupEnum.OUTPUTS_GROUP)
            ?: error("Expected a filler object")
        require(fillerOutput is ComponentPaddingConfiguration.Filler.TransactionState) { "Expected filler of type TransactionState" }
        val paddingHash =
            currentVtx.componentGroupLeafDigestService.hash(paddingNonce.bytes + fillerOutput.content.fingerprint())

        fun List<PaddingWrapper<StateRef>>.collectUtxoHashesInto(hashes: MutableList<SecureHash>) =
            mapIndexed() { index, it ->
                val collectedHash = collectStateRefUtxoHash(
                    it,
                    paddingHash = paddingHash
                )

                hashes.add(index, collectedHash)

                when (it) {
                    is PaddingWrapper.Filler -> {
                        println("   $indent Skipping input $index: padding")
                    }
                    is PaddingWrapper.Original -> {
                        println("   $indent Walking chain for StateRef $index: ${it.content.toString().take(8)}")
                        // We only recurse if we are verifying a real stateRef
                        val prevVtx = zkStorage.getTransaction(it.content.txhash) ?: error("Should not happen")
                        // TODO: Perhaps save this recursion until the end? Depends which order we want...
                        verify(prevVtx, level + 1)
                    }
                }
            }

        currentVtx.padded.inputs().collectUtxoHashesInto(inputHashes)
        currentVtx.padded.references().collectUtxoHashesInto(referenceHashes)

        val calculatedPublicInput = PublicInput(
            currentVtx.id,
            inputHashes = inputHashes,
            referenceHashes = referenceHashes
        )

        val zkTransactionService = zkTransactionServices[currentVtx.circuitId]
            ?: zkTransactionServices[SecureHash.allOnesHash]
            ?: error("Unknown circuit with id: ${currentVtx.circuitId}")

        val verifyDuration = measureTime {
            zkTransactionService.verify(currentVtx.proof, calculatedPublicInput)
        }
        println("   $indent Verified proof for tx: ${currentVtx.id.toString().take(8)} in $verifyDuration")
    }

    private fun collectStateRefUtxoHash(
        paddingWrapper: PaddingWrapper<StateRef>,
        paddingHash: SecureHash
    ): SecureHash {

        var hash = paddingHash
        if (paddingWrapper is PaddingWrapper.Original) {
            val stateRef = paddingWrapper.content
            val prevVtx = zkStorage.getTransaction(stateRef.txhash) ?: error("Should not happen")

                /*
                 * To be able to verify that the stateRefs that are used in the transaction are correct, and unchanged from
                 * when they were outputs in the previous tx, the verifier needs both the Merkle hash for each output and
                 * the nonce that was used to create those Merkle hashes.
                 *
                 * These values will be used as part of the instance when verifying the proof.
                 */
            hash = prevVtx.outputHashes[stateRef.index]

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
        }
        return hash
    }

    fun cleanup() {
        zkTransactionServices.forEach { (_, service) ->
            (service as? ZincZKTransactionService)?.cleanup()
        }
    }
}
