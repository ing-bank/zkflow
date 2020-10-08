package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.dactyloscopy.fingerprint
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.common.zkp.MockZKTransactionService
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.common.zkp.ZincZKTransactionService
import com.ing.zknotary.node.services.collectVerifiedDependencies
import com.ing.zknotary.node.services.toZKVerifierTransaction
import com.ing.zknotary.nodes.services.MockZKTransactionStorage
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.WireTransaction
import net.corda.testing.node.MockServices
import net.corda.testing.node.createMockCordaService
import net.corda.testing.node.ledger
import java.io.File
import java.nio.ByteBuffer
import java.time.Duration
import kotlin.streams.toList
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class VerificationService {
    private val ledgerServices: MockServices
    private val zkTransactionServices: Map<SecureHash, ZKTransactionService>
    private val zkStorage: MockZKTransactionStorage

    constructor(ledgerServices: MockServices, circuits: Map<SecureHash, String>) {
        this.ledgerServices = ledgerServices
        zkStorage = createMockCordaService(ledgerServices, ::MockZKTransactionStorage)

        println("Setting up ${circuits.size} circuits, this may take some minutes")
        val overallSetupDuration = measureTime {
            zkTransactionServices = circuits.entries.parallelStream().map { (circuitId, circuitPath) ->
                println("Starting for $circuitPath")

                val circuitFolder = File(circuitPath).absolutePath
                val artifactFolder = File("$circuitFolder/artifacts")
                artifactFolder.mkdirs()

                val zkTransactionService = ZincZKTransactionService(
                    circuitFolder,
                    artifactFolder = artifactFolder.absolutePath,
                    buildTimeout = Duration.ofSeconds(10 * 60),
                    setupTimeout = Duration.ofSeconds(10 * 60),
                    provingTimeout = Duration.ofSeconds(10 * 60),
                    verificationTimeout = Duration.ofSeconds(10 * 60)
                )

                val setupDuration = measureTime {
                    zkTransactionService.setup()
                }

                println("Setup duration for $circuitPath: ${setupDuration.inMinutes} minutes")

                circuitId to zkTransactionService
            }
                .toList()
                .toMap()
            // Impossible to immediately collect into a Map.
        }

        println("Overall setup duration: ${overallSetupDuration.inMinutes} minutes")
    }

    private constructor(ledgerServices: MockServices) {
        this.ledgerServices = ledgerServices
        zkTransactionServices = mapOf(SecureHash.allOnesHash as SecureHash to createMockCordaService(ledgerServices, ::MockZKTransactionService))
        zkStorage = createMockCordaService(ledgerServices, ::MockZKTransactionStorage)
    }

    companion object {
        fun mocked(ledgerServices: MockServices): VerificationService {
            return VerificationService(ledgerServices)
        }
    }

    fun verify(tx: WireTransaction) {
        ledgerServices.ledger {
            // First, if not done before,  the prover makes sure it has all SignedTransactions by calling ResolveTransactionsFlow
            // Then, the prover walks through the list of stxs in order from issuance, leading to the stx to prove,
            // and creates ptxs out of them. This requires changing txhashes in the StateRefs to the just calculated
            // txhashes of the newly created ptxs.
            val orderedDeps =
                ledgerServices.validatedTransactions.collectVerifiedDependencies(tx.inputs + tx.references)

            // Create and store all vtxs ordered from issuances up to head tx
            (orderedDeps + tx.id).forEach {
                print("Proving tx: ${it.toString().take(8)}")
                val provingTime = measureTime {
                    val stx = ledgerServices.validatedTransactions.getTransaction(it)!!

                    val commandId = ((stx.coreTransaction as WireTransaction).commands.single().value as ZKCommandData).id
                    val circuitId = SecureHash.Companion.sha256(ByteBuffer.allocate(4).putInt(commandId).array())
                    val zkTransactionService = zkTransactionServices[circuitId]
                        ?: zkTransactionServices[SecureHash.allOnesHash]
                        ?: error("Unknown circuit for command id: $commandId")

                    val vtx = stx.toZKVerifierTransaction(
                        ledgerServices,
                        zkStorage,
                        zkTransactionService,
                        persist = true
                    )

                    print(" => ${vtx.id.toString().take(8)}")
                }
                println(" in $provingTime")
            }

            println("\nStarting recursive verification:")
            verify(zkStorage.zkVerifierTransactionFor(tx)!!)
        }
    }

    // Verify each tx recursively ("walking back the chain")
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
        // (zkTransactionService as? ZincZKTransactionService)?.cleanup()
    }
}
