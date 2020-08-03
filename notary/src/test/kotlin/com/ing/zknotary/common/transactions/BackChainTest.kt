package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.zkp.MockZKService
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.node.services.collectVerifiedDependencies
import com.ing.zknotary.node.services.toZKVerifierTransaction
import com.ing.zknotary.nodes.services.MockZKTransactionStorage
import junit.framework.TestCase.assertEquals
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SingletonSerializeAsToken
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

    private val zkStorage = createMockCordaService(ledgerServices, ::MockZKTransactionStorage)
    private val zkService = createMockCordaService(ledgerServices, ::MockZKService)

    // TODO: Make this use the ZincSerializationFactory when it supports deserialization
    // private val zkSerializatinFactoryService = createMockCordaService(ledgerServices, ::ZincSerializationFactoryService)
    private val zkSerializatinFactoryService =
        createMockCordaService(ledgerServices, ::DefaultSerializationFactoryService)

    @CordaService
    class DefaultSerializationFactoryService() : SingletonSerializeAsToken(), SerializationFactoryService {

        // For CordaService. We don't need the serviceHub anyway in this Service
        constructor(serviceHub: AppServiceHub?) : this()

        override val factory: SerializationFactory
            get() = SerializationFactory.defaultFactory
    }

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
            println("ANOTHERMOVE \tWTX: ${anotherMoveWtx.id}")

            verifies()
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

            // Create and store all vtxs ordered from issuances up to head tx
            (orderedDeps + anotherMoveWtx.id).forEach {

                ledgerServices.validatedTransactions.getTransaction(it)!!
                    .toZKVerifierTransaction(
                        ledgerServices,
                        zkStorage,
                        zkService,
                        zkSerializatinFactoryService,
                        persist = true
                    )
            }

            println("\n\n\nStarting recursive verification:")
            val currentVtx = zkStorage.zkVerifierTransactionFor(anotherMoveWtx)!!
            verify(currentVtx)
        }
    }

    // Verify each tx recursively ("walkin back the chain")
    // fun verify(currentVtx: ZKVerifierTransaction, currentProof: InputsProof, level: Int = 0) {
    fun verify(currentVtx: ZKVerifierTransaction, level: Int = 0) {
        val indent = " ".repeat(level * 6) + "|-"
        println("$indent Verifying TX at level $level: ${currentVtx.id.toString().take(8)}")

        if (currentVtx.inputs.isEmpty() && currentVtx.references.isEmpty()) {
            println("   $indent No inputs and references")
        }

        // verify the tx graph for each input and collect nonces and hashes for current tx verification
        val inputNonces = mutableMapOf<Int, SecureHash>()
        val inputHashes = mutableMapOf<Int, SecureHash>()
        val referenceNonces = mutableMapOf<Int, SecureHash>()
        val referenceHashes = mutableMapOf<Int, SecureHash>()

        (currentVtx.inputs).forEachIndexed { index, stateRef ->
            println("   $indent Verifying input $index: ${stateRef.toString().take(8)}")
            val prevVtx = zkStorage.getTransaction(stateRef.txhash) ?: error("Should not happen")

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

            verify(prevVtx, level + 1)
        }

        (currentVtx.references).forEachIndexed { index, stateRef ->
            println("   $indent Verifying reference $index: ${stateRef.toString().take(8)}")
            val prevVtx = zkStorage.getTransaction(stateRef.txhash) ?: error("Should not happen")

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

            verify(prevVtx, level + 1)
        }
        val calculatedPublicInput = PublicInput(
            currentVtx.id,
            inputNonces = inputNonces,
            inputHashes = inputHashes,
            referenceNonces = referenceNonces,
            referenceHashes = referenceHashes
        )

        zkService.verify(currentVtx.proof, calculatedPublicInput.serialize(zkSerializatinFactoryService.factory).bytes)
    }
}
