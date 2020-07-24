package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.zkp.fingerprint
import com.ing.zknotary.node.services.collectVerifiedDependencies
import com.ing.zknotary.nodes.services.MockZKTransactionStorage
import junit.framework.TestCase.assertEquals
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.serialize
import net.corda.core.transactions.WireTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.createMockCordaService
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test

class BackChainTest {
    private val alice = TestIdentity.fresh("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fresh("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice,
        testNetworkParameters(minimumPlatformVersion = 6),
        bob
    )

    private lateinit var createWtx: WireTransaction
    private lateinit var moveWtx: WireTransaction
    private lateinit var anotherMoveWtx: WireTransaction

    @Before
    fun setup() {
        ledgerServices.ledger {
            createWtx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Alice's asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()

            println("CREATE \t\t\tWTX: ${createWtx.id}")
            val createdState = createWtx.outRef<TestContract.TestState>(0)

            moveWtx = transaction {
                input(createdState.ref)
                output(TestContract.PROGRAM_ID, createdState.state.data.withNewOwner(bob.party).ownableState)
                command(listOf(createdState.state.data.owner.owningKey), TestContract.Move())
                verifies()
            }
            println("MOVE \t\t\tWTX: ${moveWtx.id}")

            val movedState = moveWtx.outRef<TestContract.TestState>(0)

            val create2Wtx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Bob's reference asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()
            println("CREATE2 \t\tWTX: ${create2Wtx.id}")

            anotherMoveWtx = transaction {
                input(movedState.ref)
                output(TestContract.PROGRAM_ID, movedState.state.data.withNewOwner(alice.party).ownableState)
                command(listOf(movedState.state.data.owner.owningKey), TestContract.Move())
                reference("Bob's reference asset")
                verifies()
            }
            println("ANOTHERMOVE \tWTX: ${anotherMoveWtx.id}, size: ${createWtx.serialize().bytes.size}")

            verifies()
        }
    }

    data class InputsProofWitness(val ptx: ZKProverTransaction)
    data class InputsProofInstance(
        val currentVtxId: SecureHash,
        val inputNonces: Map<Int, SecureHash>,
        val inputHashes: Map<Int, SecureHash>,
        val referenceNonces: Map<Int, SecureHash>,
        val referenceHashes: Map<Int, SecureHash>
    )

    class InputsProof(private val witness: InputsProofWitness) {
        fun verify(instance: InputsProofInstance) {

            /*
             * Rule: witness.ptx.inputs[i] contents hashed with nonce should equal instance.prevVtxOutputHashes[i].
             * This proves that prover did not change the contents of the input states
             */
            witness.ptx.inputs.map { it.state }.forEachIndexed { index, input ->
                @Suppress("UNCHECKED_CAST")
                input as TransactionState<ZKContractState>

                assertEquals(
                    instance.inputHashes[index],
                    BLAKE2s256DigestService.hash(instance.inputNonces[index]!!.bytes + input.fingerprint)
                )
            }

            /*
             * Rule: witness.ptx.inputs[i] contents hashed with nonce should equal instance.prevVtxOutputHashes[i].
             * This proves that prover did not change the contents of the input states
             */
            witness.ptx.references.map { it.state }.forEachIndexed { index, reference ->
                @Suppress("UNCHECKED_CAST")
                reference as TransactionState<ZKContractState>

                assertEquals(
                    instance.referenceHashes[index],
                    BLAKE2s256DigestService.hash(instance.referenceNonces[index]!!.bytes + reference.fingerprint)
                )
            }

            /*
             * Rule: The recalculated Merkle root should match the one from the instance vtx.
             *
             * In this case on the Corda side, a ZKProverTransaction id is lazily recalculated always. This means it is
             * always a direct representation of the ptx contents so we don't have to do a recalculation.
             * On the Zinc side, we will need explicit recalculation based on the witness inputs.
             *
             * Here, we simply compare the witness.ptx.id with the instance.currentVtxId.
             * This proves that the inputs whose contents have been verified to be unchanged, are also part of the vtx
             * being verified.
             */
            assertEquals(instance.currentVtxId, witness.ptx.id)
        }
    }

    @Test
    fun `Prover can fetch the complete tx graph for input StateRefs`() {
        val sortedDependencies = ledgerServices.validatedTransactions
            .collectVerifiedDependencies(anotherMoveWtx.inputs)

        // We expect that the sorted deps of the anotherMoveWtx input is createWtx, moveWtx.
        assertEquals(listOf(createWtx.id, moveWtx.id), sortedDependencies)
    }

    @Test
    fun `Prover can deterministically build graph of ZKVerifierTransactions based on graph of SignedTransactions`() {
        ledgerServices.ledger {
            // First, if not done before,  the prover makes sure it has all SignedTransactions by calling ResolveTransactionsFlow
            // Then, the prover walks through the list of stxs in order from issuance, leading to the stx to prove,
            // and creates ptxs out of them. This requires changing txhashes in the StateRefs to the just calculated
            // txhashes of the newly created ptxs.
            val orderedDeps =
                ledgerServices.validatedTransactions.collectVerifiedDependencies(anotherMoveWtx.inputs + anotherMoveWtx.references)

            // val zkStorage = ledgerServices.cordaService(MockZKTransactionStorage::class.java)
            val zkStorage = createMockCordaService(ledgerServices, ::MockZKTransactionStorage)

            val vtxs = mutableMapOf<SecureHash, Pair<ZKVerifierTransaction, InputsProof>>()

            // Create all ptxs ordered from issuances to head tx
            orderedDeps.forEach {
                val wtx = checkNotNull(ledgerServices.validatedTransactions.getTransaction(it)) {
                    "Unexpectedly could not find the wtx for $it. Did you run ResolveTransactionsFlow before?"
                }.coreTransaction as WireTransaction

                println("Creating PTX for $it")
                val ptx = wtx.toZKProverTransaction(
                    ledgerServices,
                    zkStorage,
                    componentGroupLeafDigestService = BLAKE2s256DigestService,
                    nodeDigestService = BLAKE2s256DigestService
                )
                println("PTX created with id ${ptx.id} for $it")

                zkStorage.map.put(wtx, ptx)
                zkStorage.addTransaction(ptx.toZKVerifierTransaction())
                // TODO: remove this when the proof is made part of the ZKVerifierTransaction
                vtxs[ptx.id] = Pair(ptx.toZKVerifierTransaction(), InputsProof(InputsProofWitness(ptx)))
            }

            // Next: create the ptx/vtx for the current tx now that we have the dependencies done.
            val anotherMovePtx = anotherMoveWtx.toZKProverTransaction(
                ledgerServices,
                zkStorage,
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = BLAKE2s256DigestService
            )
            println("PTX created with id ${anotherMovePtx.id} for ${anotherMoveWtx.id}")

            val anotherMovePtxProof = InputsProof(InputsProofWitness(anotherMovePtx))
            zkStorage.map.put(anotherMoveWtx, anotherMovePtx)
            zkStorage.addTransaction(anotherMovePtx.toZKVerifierTransaction())
            // TODO: remove this when the proof is made part of the ZKVerifierTransaction
            vtxs[anotherMovePtx.id] = Pair(anotherMovePtx.toZKVerifierTransaction(), anotherMovePtxProof)

            // Next: verification/walking back the chain of vtxs

            // Verify each tx recursively
            fun verify(currentVtx: ZKVerifierTransaction, currentProof: InputsProof, level: Int = 0) {
                val indent = " ".repeat(level * 6) + "|-"
                println("$indent Verifying TX at level $level: ${currentVtx.id.toString().take(8)}")

                if (currentVtx.inputs.isEmpty() && currentVtx.references.isEmpty()) {
                    println("   $indent No inputs and references")
                    return
                }

                // verify the tx graph for each input and collect nonces and hashes for current tx verification
                val inputNonces = mutableMapOf<Int, SecureHash>()
                val inputHashes = mutableMapOf<Int, SecureHash>()
                val referenceNonces = mutableMapOf<Int, SecureHash>()
                val referenceHashes = mutableMapOf<Int, SecureHash>()

                (currentVtx.inputs).forEachIndexed { index, stateRef ->
                    println("   $indent Verifying input $index: ${stateRef.toString().take(8)}")
                    val prevVtx = vtxs[stateRef.txhash]?.first ?: error("Should not happen")
                    val prevProof = vtxs[stateRef.txhash]?.second ?: error("Should not happen")

                    /*
                     * To be able to verify that the stateRefs that are used in the transaction are correct, and unchanged from
                     * when they were outputs in the previous tx, the verifier needs both the Merkle hash for each output and
                     * the nonce that was used to create those Merkle hashes.
                     *
                     * These values will be used as part of the instance when verifying the proof.
                     */
                    inputNonces[index] =
                        prevVtx.componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]!![stateRef.index]
                    inputHashes[index] = prevVtx.outputHashes[stateRef.index]

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

                    verify(prevVtx, prevProof, level + 1)
                }

                (currentVtx.references).forEachIndexed { index, stateRef ->
                    println("   $indent Verifying reference $index: ${stateRef.toString().take(8)}")
                    val prevVtx = vtxs[stateRef.txhash]?.first ?: error("Should not happen")
                    val prevProof = vtxs[stateRef.txhash]?.second ?: error("Should not happen")

                    /*
                     * To be able to verify that the stateRefs that are used in the transaction are correct, and unchanged from
                     * when they were outputs in the previous tx, the verifier needs both the Merkle hash for each output and
                     * the nonce that was used to create those Merkle hashes.
                     *
                     * These values will be used as part of the instance when verifying the proof.
                     */
                    referenceNonces[index] =
                        prevVtx.componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]!![stateRef.index]
                    referenceHashes[index] = prevVtx.outputHashes[stateRef.index]

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

                    verify(prevVtx, prevProof, level + 1)
                }

                currentProof.verify(
                    InputsProofInstance(
                        currentVtxId = currentVtx.id,
                        inputHashes = inputHashes,
                        inputNonces = inputNonces,
                        referenceHashes = referenceHashes,
                        referenceNonces = referenceNonces
                    )
                )
            }

            println("\n\n\nStarting recursive verification:")
            val currentVtx = vtxs.toList().last().second
            verify(currentVtx.first, currentVtx.second)
        }
    }
}
