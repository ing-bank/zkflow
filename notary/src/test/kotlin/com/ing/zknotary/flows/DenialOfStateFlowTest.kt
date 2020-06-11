package com.ing.zknotary.flows

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.contracts.TestContract.Companion.PROGRAM_ID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import net.corda.core.contracts.Command
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.NotaryError
import net.corda.core.flows.NotaryException
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.SerializationFactory
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.core.CHARLIE_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.internal.findCordapp
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("Can be enabled if you want to show how a denial of state attack works with normal Corda. Should fail with ZKP Notary.")
class DenialOfStateFlowTest {
    private lateinit var mockNet: MockNetwork
    private lateinit var notaryNode: StartedMockNode
    private lateinit var notary: Party
    private lateinit var aliceNode: StartedMockNode
    private lateinit var alice: Party
    private lateinit var bobNode: StartedMockNode
    private lateinit var bob: Party
    private lateinit var charlieNode: StartedMockNode
    private lateinit var charlie: Party

    @Before
    fun setup() {
        mockNet = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    findCordapp("com.ing.zknotary.notary"),
                    findCordapp("com.ing.zknotary.common.contracts")
                ),
                notarySpecs = listOf(
                    MockNetworkNotarySpec(
                        name = CordaX500Name("Custom Notary", "Amsterdam", "NL"),
                        validating = false
                    )
                ),
                networkParameters = testNetworkParameters(minimumPlatformVersion = 5)
            )
        )
        aliceNode = mockNet.createPartyNode(ALICE_NAME)
        alice = aliceNode.info.singleIdentity()
        bobNode = mockNet.createPartyNode(BOB_NAME)
        bob = bobNode.info.singleIdentity()
        charlieNode = mockNet.createPartyNode(CHARLIE_NAME)
        charlie = charlieNode.info.singleIdentity()
        notaryNode = mockNet.defaultNotaryNode
        notary = mockNet.defaultNotaryIdentity

        bobNode.registerInitiatedFlow(MoveReplyFlow::class.java)
        charlieNode.registerInitiatedFlow(MoveReplyFlow::class.java)
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
    }

    @Test
        /*
         * In this version of the attack, Alice was no partiipant in any earlier tx.
         * Therefore she has no knowledge of the contents of any of these transactions or states.
         * Alice wants to maliciously prevent Bob from using his assets on the ledger.
         * Alice manages to discover the identifier of one of Bob's UTXO's.
         * Alice handcrafts a tx that consumes the UTXO.
         * This tx will of course not be signed by Bob, who is not aware of the attack.
         * Alice request notarisation for this malicious tx from a non-validating notary.
         * The notary signs the tx, because it does not check contract and sigs and it is not a double spend.
         * Bob is now blocked from using the state. When he tries to do that, the notary will reject the tx
         * as a double spend.
         */
    fun `only knowing state id is enough for denial of state attack`() {
        // Bob has a state
        val bobsState = runCreateTx(bobNode, bob).coreTransaction.outRef<TestContract.TestState>(0)

        // Alice finds out the id of Bob's state.
        val bobsStateRef = bobsState.ref

        // Alice executes a malicious tx to consume Bob's state, the notary signs it.
        val aliceConsumesTransaction = runDenialOfStateConsumeTx(aliceNode, bobsStateRef)
        val signers = aliceConsumesTransaction.sigs.map { it.by }
        assertTrue { notary.owningKey in signers }

        // Bob tries to spend it (use it as input) and it will fail
        // Charlie will accept this, as it is a valid tx chain from his perspective, but
        // the notary will not sign it, as it has already seen the input in Alice's malicious tx.
        val ex = assertFailsWith<NotaryException> {
            runMoveTx(bobNode, bobsState, charlie)
        }
        assertThat(ex.error).isInstanceOf(NotaryError.Conflict::class.java)
    }

    @Test
        /*
         * In this version of the attack, Alice is a participant in a transaction with Bob.
         * Therefore she has knowledge of the output state that was the result of that transaction.
         * Alice will try to maliciously regain ownership of the state she gave to bob.
         * Alice handcrafts a tx that assigns ownership back to her, resulting in a new output state in her name.
         * This tx will of course not be signed by Bob, who is not aware of the attack.
         * Alice request notarisation for this malicious tx from a non-validating notary.
         * The notary signs the tx, because it does not check contract and sigs and it is not a double spend.
         * Bob is now blocked from using the state. When he tries to do that, the notary will reject the tx
         * as a double spend.
         * Now Alice tries to sell the maliciously created output state to Charlie in a next tx.
         * Charlie rejects this, because even though the tx creating the state was notarised, unlike the
         * non-validating notary, Charlie **will** check the smart contract rules and sigs for all txs leading to
         * the existence of this state. Those checks will fail, because Bobs signature is missing.
         * End result: Bob is denied the usage of his state, and Alice will not be able to use it either.
         */
    fun `denial of state is successful with non-validating notary`() {
        // Alice issues a state. This is normal and notarised
        val aliceCreated = runCreateTx(aliceNode, alice)

        // Alice: execute a valid move tx to move alice's state to Bob
        // According to the contract
        val aliceMovedToBob = runMoveTx(aliceNode, aliceCreated.coreTransaction.outRef(0), bob)
        val signers = aliceMovedToBob.sigs.map { it.by }
        assertTrue {
            notary.owningKey in signers &&
                bob.owningKey in signers
        }

        // Alice: determine the stateref of the output state now owned by Bob.
        // Alice can know this in a normal situation, because she created the move tx to move her state to Bob.
        val bobsState = aliceMovedToBob.coreTransaction.outRef<TestContract.TestState>(0)

        // Alice: execute another, malicious, tx to move Bob's stateRef state back to Alice.
        // We handcraft a tx that we send directly to the notary, that transfers the state back to Alice.
        // Charlie will not accept this, but the state will be "spent" because the notary *will* sign it and commit the
        // input stateRef to its list of spent states.
        val aliceMovedToAlice = runDenialOfStateMoveTx(aliceNode, bobsState, alice)
        val signers2 = aliceMovedToAlice.sigs.map { it.by }
        assertTrue { notary.owningKey in signers2 }

        val aliceMaliciousState = aliceMovedToAlice.coreTransaction.outRef<TestContract.TestState>(0)

        // Bob tries to spend it (use it as input) and it will fail
        // Charlie will accept this, as it is a valid tx chain from his perspective, but
        // the notary will not sign it, as it has already seen the input in Alice's malicious tx.
        val ex = assertFailsWith<NotaryException> {
            runMoveTx(bobNode, bobsState, charlie)
        }
        assertThat(ex.error).isInstanceOf(NotaryError.Conflict::class.java)

        // To show that the damage is limited to only the 'locking' of Bob's state in the notary,
        // and to show that it does not include the ability for Alice to use the state for other purposes:
        // Future tx counterparties of Alice will not accept the chain of txs leading to this state, because it
        // never was a valid tx: Bob should have signed it and didn't.
        // It is only the non-validating notary that does not check for that.
        val charlieException = assertFailsWith<TransactionVerificationException.ContractRejection> {
            runMoveTx(aliceNode, aliceMaliciousState, charlie)
        }
        assertEquals(
            aliceMovedToAlice.id,
            charlieException.txId,
            "Expected Alice's malicious transaction to fail verification by Charlie"
        )
    }
    private fun runDenialOfStateConsumeTx(
        attackerNode: StartedMockNode,
        stateRefToDeny: StateRef
    ): SignedTransaction {
        val attackerPubKey = attackerNode.info.singleIdentity().owningKey
        val wireTx = SerializationFactory.defaultFactory.withCurrentContext(null) {
            WireTransaction(
                createComponentGroups(
                    inputs = listOf(stateRefToDeny),
                    outputs = emptyList(),
                    notary = notary,
                    attachments = listOf(SecureHash.zeroHash),
                    commands = listOf(Command(TestContract.Move(), attackerPubKey)),
                    networkParametersHash = attackerNode.services.networkParametersService.currentHash,
                    timeWindow = null,
                    references = emptyList()
                ),
                PrivacySalt()
            )
        }
        val signatureMetadata = SignatureMetadata(
            5,
            Crypto.findSignatureScheme(attackerPubKey).schemeNumberID
        )
        val signableData = SignableData(wireTx.id, signatureMetadata)
        val sig = attackerNode.services.keyManagementService.sign(signableData, attackerPubKey)
        val stx = SignedTransaction(wireTx, listOf(sig))

        val notaryFuture = attackerNode.startFlow(NonTxCheckingNotaryClientFlow(stx))
        mockNet.runNetwork()
        return stx + notaryFuture.getOrThrow()
    }

    private fun runDenialOfStateMoveTx(
        attackerNode: StartedMockNode,
        inputOwnedBySomeoneElse: StateAndRef<TestContract.TestState>,
        newOwner: Party
    ): SignedTransaction {
        val stx =
            attackerNode.services.signInitialTransaction(buildMoveTxForDenialOfState(inputOwnedBySomeoneElse, newOwner))

        // We skip collecting signatures from the counterparty, and directly notarise, because the non-validating does not check signatures anyway.
        // Also, the counterparty (if it is not the attacker) would reject this, because they do resolve the tx chain, verify the contract and the signatures.
        // That would fail, because the input state for the dos-transaction tx was not owned by us and the tx was not signed by the owner (bob).
        val notaryFuture = attackerNode.startFlow(NonTxCheckingNotaryClientFlow(stx))
        mockNet.runNetwork()
        val notarySignedTx = stx + notaryFuture.getOrThrow()

        // Alice needs to store the malicious tx to allow counterparties to fetch later when resolving the chain.
        // A counterparty will then reject this tx, because it was not signed by the owner of the input state.
        // But if we don't store it, the counterparty will fail even faster when trying to fetch the tx from us.
        attackerNode.services.recordTransactions(notarySignedTx)

        return notarySignedTx
    }

    private fun buildMoveTxForDenialOfState(
        inputOwnedBySomeoneElse: StateAndRef<TestContract.TestState>,
        attacker: Party
    ): TransactionBuilder {
        return TransactionBuilder(inputOwnedBySomeoneElse.state.notary)
            .addInputState(inputOwnedBySomeoneElse)
            .addOutputState(inputOwnedBySomeoneElse.state.data.copy(owner = attacker), PROGRAM_ID)
            // Even though the contract and required sigs are not verified by the non-validating notary,
            // we set only our key as required to prevent some annoying local exceptions during tx creation that
            // are caused by us verifying our own tx during txbuilder->signedtx transition.
            .addCommand(TestContract.Move(), attacker.owningKey)
    }

    private fun runMoveTx(
        node: StartedMockNode,
        input: StateAndRef<TestContract.TestState>,
        newOwner: Party
    ): SignedTransaction {
        val tx = buildMoveTx(input, newOwner)
        val stx = node.services.signInitialTransaction(tx)
        val moveFuture = node.startFlow(MoveFlow(stx, newOwner, FinalityFlow::class))
        mockNet.runNetwork()
        return moveFuture.getOrThrow()
    }

    private fun buildMoveTx(input: StateAndRef<TestContract.TestState>, newOwner: Party): TransactionBuilder {
        return TransactionBuilder(input.state.notary)
            .addInputState(input)
            .addOutputState(input.state.data.copy(owner = newOwner), PROGRAM_ID)
            .addCommand(TestContract.Move(), input.state.data.owner.owningKey, newOwner.owningKey)
    }

    private fun runCreateTx(ownerNode: StartedMockNode, owner: Party): SignedTransaction {
        val tx = buildCreateTx(owner)
        val stx = ownerNode.services.signInitialTransaction(tx)
        val future = ownerNode.startFlow(FinalityFlow(stx, emptyList()))
        mockNet.runNetwork()
        return future.getOrThrow()
    }

    private fun buildCreateTx(owner: Party): TransactionBuilder {
        return TransactionBuilder(notary)
            .addOutputState(TestContract.TestState(owner), PROGRAM_ID)
            .addCommand(TestContract.Create(), owner.owningKey)
    }
}
