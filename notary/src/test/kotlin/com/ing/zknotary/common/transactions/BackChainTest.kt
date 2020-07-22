package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.zkp.fingerprint
import junit.framework.TestCase.assertEquals
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.dependencies
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.node.services.DbTransactionsResolver
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test
import java.util.LinkedHashSet

class BackChainTest {
    private val alice = TestIdentity.fresh("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fresh("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    private lateinit var createWtx: WireTransaction

    // private lateinit var createVtx: ZKVerifierTransaction
    // private lateinit var moveVtx: ZKVerifierTransaction
    // private lateinit var movePtx: ZKProverTransaction
    private lateinit var moveWtx: WireTransaction
    private lateinit var anotherMoveWtx: WireTransaction
    // private lateinit var anotherMovePtx: ZKProverTransaction
    // private lateinit var anotherMoveVtx: ZKVerifierTransaction

    @Before
    fun setup() {
        ledgerServices.ledger {
            createWtx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Alice's asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()
            println("CREATED WTX: ${createWtx.id}, size: ${createWtx.serialize().bytes.size}")
            val createdState = createWtx.outRef<TestContract.TestState>(0)

            // // when creating a ptx from a wiretransaction, we need to look up the ZKVerifiertransaction that belongs
            // // to it, so we can switch the input and reference stateRefs for the ones at that location in the vtx
            // val createPtx = ZKProverTransactionFactory.create(
            //     createWtx.toLedgerTransaction(ledgerServices),
            //     componentGroupLeafDigestService = BLAKE2s256DigestService,
            //     nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            // )
            //
            //
            // // build filtered ZKVerifierTransaction
            // createVtx = createPtx.toZKVerifierTransaction()
            // println("CREATED VTX: ${createVtx.id}, size: ${createVtx.serialize().bytes.size}")

            moveWtx = transaction {
                input(createdState.ref)
                output(TestContract.PROGRAM_ID, createdState.state.data.withNewOwner(bob.party).ownableState)
                command(listOf(createdState.state.data.owner.owningKey), TestContract.Move())
                verifies()
            }
            println("CREATED WTX: ${moveWtx.id}, size: ${moveWtx.serialize().bytes.size}")

            // // Build a ZKProverTransaction
            // movePtx = ZKProverTransactionFactory.create(
            //     moveWtx.toLedgerTransaction(ledgerServices),
            //     componentGroupLeafDigestService = BLAKE2s256DigestService,
            //     nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            // )
            //
            // // build filtered ZKVerifierTransaction
            // moveVtx = movePtx.toZKVerifierTransaction()
            // println("CREATED VTX: ${moveVtx.id}, size: ${moveVtx.serialize().bytes.size}")

            val movedState = moveWtx.outRef<TestContract.TestState>(0)

            anotherMoveWtx = transaction {
                input(movedState.ref)
                output(TestContract.PROGRAM_ID, movedState.state.data.withNewOwner(alice.party).ownableState)
                command(listOf(movedState.state.data.owner.owningKey), TestContract.Move())
                verifies()
            }
            println("CREATED WTX: ${anotherMoveWtx.id}, size: ${createWtx.serialize().bytes.size}")

            verifies()

            // // Build a ZKProverTransaction
            // anotherMovePtx = ZKProverTransactionFactory.create(
            //     anotherMoveWtx.toLedgerTransaction(ledgerServices),
            //     componentGroupLeafDigestService = BLAKE2s256DigestService,
            //     nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            // )
            //
            // // build filtered ZKVerifierTransaction
            // anotherMoveVtx = anotherMovePtx.toZKVerifierTransaction()
            // println("CREATED VTX: ${anotherMoveVtx.id}, size: ${createVtx.serialize().bytes.size}")
        }
    }

    data class InputsProofWitness(val ptx: ZKProverTransaction)
    data class InputsProofInstance(
        val currentVtxId: SecureHash,
        val inputNonces: Map<Int, SecureHash>,
        val inputHashes: Map<Int, SecureHash>
        // val prevVtxOutputNonces: Map<Int, List<SecureHash>>,
        // val prevVtxOutputHashes: Map<Int, List<SecureHash>>
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

    /**
     * Returns the chain of transactions for this transaction id
     */
    private fun loadChainFromVault(
        id: SecureHash
    ): List<SignedTransaction> {
        val tx = ledgerServices.validatedTransactions.getTransaction(id)!!
        val depTxs = tx.dependencies.flatMap { loadChainFromVault(it) }

        return depTxs + tx
    }

    /**
     * Collects all verified transaction chains from local storage for each StateRef provided.
     * This should always be called after ResolveTransactionsFlow, to prevent an exception because of a missing transaction
     */
    fun collectVerifiedDependencies(
        stateRefs: List<StateRef>,
        serviceHub: ServiceHub,
        block: ((stx: SignedTransaction) -> Unit)? = null
    ): List<SecureHash> {
        val txHashes = stateRefs.map { it.txhash }

        // Keep things unique but ordered, for unit test stability.
        val nextRequests = LinkedHashSet<SecureHash>(txHashes)
        val topologicalSort = DbTransactionsResolver.TopologicalSort()

        while (nextRequests.isNotEmpty()) {
            // Don't re-fetch the same tx when it's referenced multiple times in the graph we're traversing.
            nextRequests.removeAll(topologicalSort.transactionIds)
            if (nextRequests.isEmpty()) {
                break
            }

            val txFromDB = checkNotNull(serviceHub.validatedTransactions.getTransaction(nextRequests.first())) {
                "Transaction with id ${nextRequests.first()} is missing from local storage. " +
                    "Please download it from peers with ResolveTransactionsFlow before using this function"
            }

            if (block != null) block(txFromDB)

            val dependencies = txFromDB.dependencies
            topologicalSort.add(txFromDB.id, dependencies)
            nextRequests.addAll(dependencies)
        }
        return topologicalSort.complete()
    }

    @Test
    fun `Prover can fetch the complete tx graph for input StateRefs`() {
        val sortedDependencies = collectVerifiedDependencies(anotherMoveWtx.inputs, ledgerServices)

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
            val wtxs = mutableMapOf<SecureHash, WireTransaction>()

            val orderedDeps = collectVerifiedDependencies(anotherMoveWtx.inputs, ledgerServices) {
                wtxs[it.id] = it.coreTransaction as WireTransaction
            }

            val vtxs = mutableMapOf<SecureHash, Pair<ZKVerifierTransaction, InputsProof>>()

            // Map from SignedTransaction.id to ZKProverTransaction.id for later lookup
            val txIdMap = mutableMapOf<SecureHash, SecureHash>()

            orderedDeps.forEach {
                println("\n\nCreating PTX for $it")
                val wtx = checkNotNull(wtxs[it]) {
                    "Unexpectedly could not find the wtx for $it. Did you run ResolveTransactionsFlow before?"
                }

                val mappedInputs = mutableListOf<StateRef>()
                if (wtx.inputs.isNotEmpty()) {
                    println("Inputs found, mapping txhash element...")
                    wtx.inputs.forEachIndexed { index, input ->
                        val zkid = checkNotNull(txIdMap[input.txhash]) {
                            "Unexpectedly could not find the tx id map for ${input.txhash}. Did you run ResolveTransactionsFlow before?"
                        }
                        mappedInputs.add(index, StateRef(zkid, input.index))
                    }
                    println("MAPPED: ")
                    println(wtx.inputs)
                    println(mappedInputs)
                }

                val ptx = wtx.toZKProverTransaction(
                    ledgerServices,
                    txIdMap,
                    componentGroupLeafDigestService = BLAKE2s256DigestService,
                    nodeDigestService = BLAKE2s256DigestService
                )
                println("CREATED PTX with id ${ptx.id}")
                if (ptx.inputs.isNotEmpty()) {
                    println("Mapped inputs:")
                    println(ptx.inputs.map { input -> input.ref })
                }

                txIdMap[wtx.id] = ptx.id
                vtxs[ptx.id] = Pair(ptx.toZKVerifierTransaction(), InputsProof(InputsProofWitness(ptx)))
            }

            // Next: create the ptx/vtx for the current tx now that we have the dependencies done.
            val anotherMovePtx = anotherMoveWtx.toZKProverTransaction(
                ledgerServices,
                txIdMap,
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = BLAKE2s256DigestService
            )
            println("CREATED anotherMovePtx with id ${anotherMovePtx.id}")
            if (anotherMovePtx.inputs.isNotEmpty()) {
                println("Mapped inputs:")
                println(anotherMovePtx.inputs.map { input -> input.ref })
            }

            val anotherMovePtxProof = InputsProof(InputsProofWitness(anotherMovePtx))
            vtxs[anotherMovePtx.id] = Pair(anotherMovePtx.toZKVerifierTransaction(), anotherMovePtxProof)

            // Next: verification/walking back the chain of vtxs

            // Verify each tx recursively
            fun verify(currentVtx: ZKVerifierTransaction, currentProof: InputsProof) {
                println("Verifying TX: ${currentVtx.id}")

                if (currentVtx.inputs.isEmpty()) {
                    println("Reached issuance transaction. Verification complete")
                    return
                }

                // verify the tx graph for each input and collect nonces and hashes for current tx verification
                val inputNonces = mutableMapOf<Int, SecureHash>()
                val inputHashes = mutableMapOf<Int, SecureHash>()

                currentVtx.inputs.forEachIndexed { index, input ->
                    println("  - Verifying input $index: \n      $input")
                    val prevVtx = vtxs[input.txhash]?.first ?: error("Should not happen")

                    /*
                     * To be able to verify that the inputs that are used in the transaction are correct, and unchanged from
                     * when they were outputs in the previous tx, the verifier needs both the Merkle hash for each output and
                     * the nonce that was used to create those Merkle hashes.
                     *
                     * These values will be used as part of the instance when verifying the proof.
                     */
                    inputNonces[index] =
                        prevVtx.componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]!![input.index]
                    inputHashes[index] = prevVtx.outputHashes[input.index]

                    /*
                     * Now the verifier calls currentVtx.proof.verify(currentVtx.id, prevVtx.outputHashes, prevVtx.outputNonces).
                     *
                     * Inside the circuit, the prover proves:
                     * - witnessTx.inputs[i] contents hashed with nonce should equal instance.moveTxInputHashesFromPrevTx[i].
                     *   This proves that prover did not change the contents of the state
                     * - Recalculates witnessTx merkleRoot based on all components from the witness, including witnessTx.inputs.
                     * - witnessTx.merkleRoot == instance.moveTx.id. This proves the witnessTx is the same as the ZKVerifierTransaction
                     *   that the verifier is trying to verify. It also proves that the inputs consumed are indeed part of the
                     *   transaction identified by the instance.
                     */

                    val prevProof = vtxs[currentVtx.inputs.single().txhash]?.second ?: error("Should not happen")

                    verify(prevVtx, prevProof)
                }

                currentProof.verify(
                    InputsProofInstance(
                        currentVtxId = currentVtx.id,
                        inputHashes = inputHashes,
                        inputNonces = inputNonces
                        // prevVtxOutputNonces = prevVtxOutputNonces,
                        // prevVtxOutputHashes = prevVtxOutputHashes
                    )
                )
            }

            println("\n\n\nStarting recursive verification:")
            val currentVtx = vtxs.toList().last().second
            verify(currentVtx.first, currentVtx.second)
        }
    }

    // @Test
    // fun `Verifier follows State chain and confirms input content is unchanged`() {
    //     ledgerServices.ledger {
    //         /*
    //          * The verifier receives the following from the prover:
    //          * - A ZKVerifierTransaction (vtx) for the current transaction that they want to have verified
    //          * - A Zero knowledge proof for this vtx, proving that we know a valid ptx matching its id and its back chain
    //          */
    //         val currentVtx = anotherMoveVtx
    //         val currentProof = InputsProof(InputsProofWitness(anotherMovePtx))
    //
    //         /*
    //          * First, the verifier resolves the chain of ZKVerifierTransactions for each input.
    //          * In this case, the moveVtx has one input, and the previous transaction is createVtx.
    //          *
    //          * Like normal transaction chain resolution, this would be done by requesting the back chain from the party
    //          * that is asking for verification. For each input. For now we will work with only one input, until we have
    //          * all logic working.
    //          */
    //
    //         // Rebuilding the list of vtxs from verified SignedTransactions stored in the local vault for the inputs
    //         // of the head transaction
    //         val wtxs = mutableMapOf<SecureHash, WireTransaction>()
    //         val vtxs = mutableMapOf<SecureHash, Pair<ZKVerifierTransaction, InputsProof>>()
    //
    //         // Map from SignedTransaction.id to ZKProverTransaction.id for later lookup
    //         val txIdMap = mutableMapOf<SecureHash, SecureHash>()
    //
    //         // This will be very naive: the prover will recalculate everyting and even recreate the ZKPs.
    //         // In end state, the prover will receive/request the preceding ZKtxs and proofs from their counterparty.
    //         collectVerifiedDependencies(currentVtx.inputs, ledgerServices) {
    //             val wtx = it.coreTransaction as WireTransaction
    //             wtxs[wtx.id] = wtx
    //         }
    //
    //         println("Collected Deps in order:")
    //         wtxs.forEach { println(it.key) }
    //
    //         // collectVerifiedDependencies(currentVtx.inputs, ledgerServices) {
    //         //     val wtx = it.coreTransaction as WireTransaction
    //         //     println("Creating PTX for ${wtx.id}")
    //         //     println("Current map contents: $txIdMap")
    //         //
    //         //     val inputs = mutableListOf<StateRef>()
    //         //     if (it.inputs.isNotEmpty()) {
    //         //         it.inputs.forEachIndexed { index, input ->
    //         //             val zkid = checkNotNull(txIdMap[input.txhash]) {
    //         //                 "Unexpectedly could not find the tx id map for ${input.txhash}. Did you run ResolveTransactionsFlow before?"
    //         //             }
    //         //             inputs[index] = StateRef(zkid, input.index)
    //         //         }
    //         //     }
    //         //
    //         //     println(it.inputs)
    //         //     println(inputs)
    //         //     val ptx = ZKProverTransactionFactory.create(
    //         //         wtx.toLedgerTransaction(ledgerServices),
    //         //         componentGroupLeafDigestService = BLAKE2s256DigestService,
    //         //         nodeDigestService = BLAKE2s256DigestService
    //         //     )
    //         //
    //         //     val proof = InputsProof(InputsProofWitness(ptx))
    //         //
    //         //     txIdMap[it.id] = ptx.id
    //         //     vtxs[ptx.id] = Pair(ptx.toZKVerifierTransaction(), proof)
    //         // }
    //
    //         // Verify each tx recursively
    //         fun verify(currentVtx: ZKVerifierTransaction, currentProof: InputsProof) {
    //             println("Verifying TX: ${currentVtx.id}")
    //
    //             if (currentVtx.inputs.isEmpty()) {
    //                 println("Reached issuance transaction. Verification complete")
    //                 return
    //             }
    //
    //             // verify the tx graph for each input and collect nonces and hashes for current tx verification
    //             val inputNonces = mutableMapOf<Int, SecureHash>()
    //             val inputHashes = mutableMapOf<Int, SecureHash>()
    //
    //             currentVtx.inputs.forEachIndexed { index, input ->
    //                 println("  - Verifying input $index: \n      $input")
    //                 val prevVtx = vtxs[input.txhash]?.first ?: error("Should not happen")
    //
    //                 /*
    //                  * To be able to verify that the inputs that are used in the transaction are correct, and unchanged from
    //                  * when they were outputs in the previous tx, the verifier needs both the Merkle hash for each output and
    //                  * the nonce that was used to create those Merkle hashes.
    //                  *
    //                  * These values will be used as part of the instance when verifying the proof.
    //                  */
    //                 inputNonces[index] =
    //                     prevVtx.componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]!![input.index]
    //                 inputHashes[index] = prevVtx.outputHashes[input.index]
    //
    //                 /*
    //                  * Now the verifier calls currentVtx.proof.verify(currentVtx.id, prevVtx.outputHashes, prevVtx.outputNonces).
    //                  *
    //                  * Inside the circuit, the prover proves:
    //                  * - witnessTx.inputs[i] contents hashed with nonce should equal instance.moveTxInputHashesFromPrevTx[i].
    //                  *   This proves that prover did not change the contents of the state
    //                  * - Recalculates witnessTx merkleRoot based on all components from the witness, including witnessTx.inputs.
    //                  * - witnessTx.merkleRoot == instance.moveTx.id. This proves the witnessTx is the same as the ZKVerifierTransaction
    //                  *   that the verifier is trying to verify. It also proves that the inputs consumed are indeed part of the
    //                  *   transaction identified by the instance.
    //                  */
    //
    //                 val prevProof = vtxs[currentVtx.inputs.single().txhash]?.second ?: error("Should not happen")
    //
    //                 verify(prevVtx, prevProof)
    //             }
    //
    //             currentProof.verify(
    //                 InputsProofInstance(
    //                     currentVtxId = currentVtx.id,
    //                     inputHashes = inputHashes,
    //                     inputNonces = inputNonces
    //                     // prevVtxOutputNonces = prevVtxOutputNonces,
    //                     // prevVtxOutputHashes = prevVtxOutputHashes
    //                 )
    //             )
    //         }
    //
    //         verify(currentVtx, currentProof)
    //     }
    // }
}
