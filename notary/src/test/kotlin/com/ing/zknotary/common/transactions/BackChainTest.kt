package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.zkp.fingerprint
import junit.framework.TestCase.assertEquals
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.dependencies
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.LedgerDSL
import net.corda.testing.dsl.TestLedgerDSLInterpreter
import net.corda.testing.dsl.TestTransactionDSLInterpreter
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test

class BackChainTest {
    private val alice = TestIdentity.fresh("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fresh("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    private lateinit var createWtx: WireTransaction
    private lateinit var createVtx: ZKVerifierTransaction
    private lateinit var moveVtx: ZKVerifierTransaction
    private lateinit var movePtx: ZKProverTransaction
    private lateinit var moveWtx: WireTransaction
    private lateinit var anotherMoveWtx: WireTransaction

    private lateinit var ledger: LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>

    @Before
    fun setup() {
        ledger = ledgerServices.ledger {
            createWtx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Alice's asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()

            val createPtx = ZKProverTransactionFactory.create(
                createWtx.toLedgerTransaction(ledgerServices),
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            )

            val createdState = createWtx.outRef<TestContract.TestState>(0)

            // build filtered ZKVerifierTransaction
            createVtx = createPtx.toZKVerifierTransaction()

            moveWtx = transaction {
                input(createdState.ref)
                output(TestContract.PROGRAM_ID, createdState.state.data.withNewOwner(bob.party).ownableState)
                command(listOf(createdState.state.data.owner.owningKey), TestContract.Move())
                verifies()
            }

            // Build a ZKProverTransaction
            movePtx = ZKProverTransactionFactory.create(
                moveWtx.toLedgerTransaction(ledgerServices),
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            )

            // build filtered ZKVerifierTransaction
            moveVtx = movePtx.toZKVerifierTransaction()

            val movedState = moveWtx.outRef<TestContract.TestState>(0)

            anotherMoveWtx = transaction {
                input(movedState.ref)
                output(TestContract.PROGRAM_ID, movedState.state.data.withNewOwner(alice.party).ownableState)
                command(listOf(movedState.state.data.owner.owningKey), TestContract.Move())
                verifies()
            }

            verifies()
        }
    }

    data class InputsProofWitness(val ptx: ZKProverTransaction)
    data class InputsProofInstance(
        val currentVtxId: SecureHash,
        val prevVtxOutputNonces: List<SecureHash>,
        val prevVtxOutputHashes: List<SecureHash>
    )

    class InputsProof(private val witness: InputsProofWitness) {
        fun verify(instance: InputsProofInstance) {

            /*
             * Rule: witness.ptx.inputs[i] contents hashed with nonce should equal instance.prevVtxOutputHashes[i].
             * This proves that prover did not change the contents of the input states
             */
            witness.ptx.inputs.map { it.state }.forEachIndexed { i, state ->
                @Suppress("UNCHECKED_CAST")
                state as TransactionState<ZKContractState>

                assertEquals(
                    instance.prevVtxOutputHashes[i],
                    BLAKE2s256DigestService.hash(instance.prevVtxOutputNonces[i].bytes + state.fingerprint)
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

    @Test
    fun `Prover can build chain of ZKVerifierTransactions based on chain of WireTransactions`() {
        ledgerServices.ledger {
            val stxs = loadChainFromVault(anotherMoveWtx.id)

            // This will be very naive: the prover will recalculate everyting and even recreate the ZKPs.
            // In end state, the prover will receive/request the preceding ZKtxs and proofs from their counterparty.
            val vtxs: List<ZKVerifierTransaction> = stxs.map {
                val wtx = it.coreTransaction as WireTransaction
                ZKProverTransactionFactory.create(
                    wtx.toLedgerTransaction(ledgerServices),
                    componentGroupLeafDigestService = BLAKE2s256DigestService,
                    nodeDigestService = BLAKE2s256DigestService
                ).toZKVerifierTransaction()
            }

            // The first transaction in the list should be the create transaction
            assertEquals(createVtx, vtxs.first())

            // the second tx in the list should be the first move tx
            assertEquals(moveVtx, vtxs[1])
        }
    }

    @Test
    fun `Verifier follows State chain and confirms input content is unchanged`() {
        ledger.apply {
            /*
             * The verifier receives the following from the prover:
             * - A ZKVerifierTransaction (vtx) for the current transaction that they want to have verified
             * - A Zero knowledge proof for this vtx, proving that we know a valid ptx matching its id and its back chain
             */
            val currentVtx = moveVtx
            val proof = InputsProof(InputsProofWitness(movePtx))

            /*
             * First, the verifier resolves the chain of ZKVerifierTransactions for each input.
             * In this case, the moveVtx has one input, and the previous transaction is createVtx.
             *
             * Like normal transaction chain resolution, this would be done by requesting the back chain from the party
             * that is asking for verification. For each input. For now we will work with only one input, until we have
             * all logic working.
             */
            val prevVtx = createVtx

            /*
             * To be able to verify that the inputs that are used in the transaction are correct, and unchanged from
             * when they were outputs in the previous tx, the verifier needs both the Merkle hash for each output and
             * the nonce that was used to create those Merkle hashes.
             *
             * These values will be used as part of the instance when verifying the proof.
             */
            val prevVtxOutputNonces = prevVtx.componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]!!
            val prevVtxOutputHashes = prevVtx.outputHashes

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
            proof.verify(
                InputsProofInstance(
                    currentVtxId = currentVtx.id,
                    prevVtxOutputNonces = prevVtxOutputNonces,
                    prevVtxOutputHashes = prevVtxOutputHashes
                )
            )
        }
    }
}
