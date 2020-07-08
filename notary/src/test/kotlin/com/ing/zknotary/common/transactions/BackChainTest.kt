package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.zkp.fingerprint
import junit.framework.TestCase.assertEquals
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.testing.core.TestIdentity
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

    private lateinit var createVtx: ZKVerifierTransaction
    private lateinit var moveVtx: ZKVerifierTransaction
    private lateinit var movePtx: ZKProverTransaction

    @Before
    fun setup() {
        ledgerServices.ledger {
            val createTx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Alice's asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()

            val createPtx = ZKProverTransactionFactory.create(
                createTx.toLedgerTransaction(ledgerServices),
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            )

            // build filtered ZKVerifierTransaction
            createVtx = createPtx.toZKVerifierTransaction()

            val createdState = createTx.outRef<TestContract.TestState>(0)

            val moveTx = transaction {
                input(createdState.ref)
                output(TestContract.PROGRAM_ID, createdState.state.data.withNewOwner(bob.party).ownableState)
                command(listOf(createdState.state.data.owner.owningKey), TestContract.Move())
                verifies()
            }

            // Build a ZKProverTransaction
            movePtx = ZKProverTransactionFactory.create(
                moveTx.toLedgerTransaction(ledgerServices),
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            )

            // build filtered ZKVerifierTransaction
            moveVtx = movePtx.toZKVerifierTransaction()
        }
    }

    @Test
    fun `Verifier follows State chain and confirms input content is unchanged`() {
        ledgerServices.ledger {
            /*
             * First, the verifier resolves the chain of ZKVerifierTransactions for each input.
             * In this case, the moveVtx has one input, and the previous transaction is createVtx
             */
            val prevVtx = createVtx
            val prevVtxOutputNonces = prevVtx.componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]!!
            val prevVtxOutputHashes = prevVtx.outputHashes

            /*
             * Now the verifier calls currentVtx.proof.verify(currentVtx.id, prevVtx.outputHashes, prevVtx.outputNonces).
             *
             * Inside the circuit, the prover proves:
             * - witnessTx.inputs[i] contents hashed with nonce should equal instance.moveTxInputHashesFromPrevTx[i].
             *   This proves that prover did not change the contents of the state
             * - witnessTx.merkeRoot == intance.moveTx.id. This proves the witnessTx is the same as the ZKVerifierTransaction
             *   that the verifier is trying to verify.
             */

            // The full ZKProverTransaction for the Move transaction (the head tx) is the witness for the current tx.
            val currentTxWitness = movePtx
            val witnessInputs = currentTxWitness.inputs.map {
                @Suppress("UNCHECKED_CAST")
                it.state as TransactionState<ZKContractState>
            }

            // Recalculate the componentHashes for each input from the witness and compare with provided prevTxOutputHashes from instance
            witnessInputs.forEachIndexed { i, state ->
                assertEquals(
                    prevVtxOutputHashes[i],
                    BLAKE2s256DigestService.hash(prevVtxOutputNonces[i].bytes + state.fingerprint)
                )
            }
        }
    }
}
