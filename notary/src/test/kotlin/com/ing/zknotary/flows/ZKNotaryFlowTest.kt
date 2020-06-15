package com.ing.zknotary.flows

import com.ing.zknotary.client.flows.ZKFinalityFlow
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.contracts.TestContract.Companion.PROGRAM_ID
import com.ing.zknotary.common.flows.getCordaServiceFromConfig
import com.ing.zknotary.common.serializer.ZKJsonSerializationFactoryService
import com.ing.zknotary.common.zkp.NoopZKVerifierService
import com.ing.zknotary.common.zkp.NoopZKZKProverService
import com.ing.zknotary.common.zkp.ZKConfig
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.internal.findCordapp
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Ignore("Can be enabled after https://github.com/ingzkp/zk-notary/issues/14")
class ZKNotaryFlowTest {
    private lateinit var mockNet: MockNetwork
    private lateinit var notaryNode: StartedMockNode
    private lateinit var notary: Party
    private lateinit var aliceNode: StartedMockNode
    private lateinit var alice: Party
    private lateinit var bobNode: StartedMockNode
    private lateinit var bob: Party

    @Before
    fun setup() {
        mockNet = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    findCordapp("com.ing.zknotary.common.contracts").withConfig(
                        mapOf(
                            "zkpSerializationFactoryService" to ZKJsonSerializationFactoryService::class.qualifiedName!!,
                            "zkpVerifier" to NoopZKVerifierService::class.qualifiedName!!,
                            "zkpProver" to NoopZKZKProverService::class.qualifiedName!!
                        )
                    )
                ),
                notarySpecs = listOf(
                    MockNetworkNotarySpec(
                        name = CordaX500Name("Custom Notary", "Amsterdam", "NL"),
                        className = "com.ing.zknotary.notary.ZKNotaryService",
                        validating = false
                        // Here we should be able to add NotaryConfig.extraConfig that is passed on all the way to the construction of the NotaryService.
                    )
                ),
                networkParameters = testNetworkParameters(minimumPlatformVersion = 6)
            )
        )
        aliceNode = mockNet.createPartyNode(ALICE_NAME)
        bobNode = mockNet.createPartyNode(BOB_NAME)
        notaryNode = mockNet.defaultNotaryNode
        notary = mockNet.defaultNotaryIdentity
        alice = aliceNode.info.singleIdentity()
        bob = bobNode.info.singleIdentity()

        bobNode.registerInitiatedFlow(MoveReplyFlow::class.java)
        bobNode.registerInitiatedFlow(ZKMoveReplyFlow::class.java)
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
    }

    @Test
    fun `valid zk create tx is notarised and persisted by creator`() {
        val stx = runCreateTx(aliceNode, alice)
        assertTrue("custom notary should sign a valid tx") {
            stx.sigs.any { it.by == notary.owningKey }
        }
        aliceNode.transaction {
            assertEquals(stx, aliceNode.services.validatedTransactions.getTransaction(stx.id))
        }
    }

    @Test
    fun `valid zk move tx is notarised and persisted by all participants`() {
        val createdStateAndRef = runCreateTx(aliceNode, alice).coreTransaction.outRef<TestContract.TestState>(0)

        val stx = runMoveTx(aliceNode, buildValidMoveTx(createdStateAndRef, bob), bob)

        val signers = stx.sigs.map { it.by }
        assertTrue {
            notary.owningKey in signers &&
                bob.owningKey in signers
        }

        aliceNode.transaction {
            assertEquals(stx, aliceNode.services.validatedTransactions.getTransaction(stx.id))
        }
        bobNode.transaction {
            assertEquals(stx, bobNode.services.validatedTransactions.getTransaction(stx.id))
        }
    }

    @Test
    @Ignore("This tx is now successful because the non-validating notary does not validate the tx. This should fail when it verifies the ZK proof.")
    fun `invalid zk move tx (contract violation) is rejected by the notary`() {
        val createdStateAndRef = runCreateTx(aliceNode, alice).coreTransaction.outRef<TestContract.TestState>(0)
        val stx = aliceNode.services.signInitialTransaction(buildContractViolatingMoveTx(createdStateAndRef, bob))

        val notaryFuture = aliceNode.startFlow(ZKNonTxCheckingNotaryClientFlow(stx, loadZKConfig(aliceNode.services)))

        mockNet.runNetwork()
        val notarySignedTx = notaryFuture.getOrThrow()
        val signers = notarySignedTx.map { it.by }
        assertTrue {
            notary.owningKey in signers
        }
    }

    private fun runMoveTx(
        node: StartedMockNode,
        tx: TransactionBuilder,
        newOwner: Party
    ): SignedTransaction {
        val stx = node.services.signInitialTransaction(tx)
        val moveFuture = node.startFlow(
            ZKMoveFlow(
                stx,
                newOwner,
                ZKConfig(
                    proverService = node.services.getCordaServiceFromConfig("zkpProver"),
                    verifierService = node.services.getCordaServiceFromConfig("zkpVerifier"),
                    serializationFactoryService = node.services.getCordaServiceFromConfig("zkpSerializationFactoryService")
                )

            )
        )
        mockNet.runNetwork()
        return moveFuture.getOrThrow()
    }

    /**
     * This tx violates the contract rule that the value of the input and output must be identical.
     */
    private fun buildContractViolatingMoveTx(
        input: StateAndRef<TestContract.TestState>,
        newOwner: Party
    ): TransactionBuilder {
        val oldValue = input.state.data.value
        val newValue = if (oldValue == Int.MAX_VALUE) oldValue - 1 else oldValue + 1
        return TransactionBuilder(input.state.notary)
            .addInputState(input)
            .addOutputState(input.state.data.copy(owner = newOwner, value = newValue), PROGRAM_ID)
            .addCommand(TestContract.Move(), input.state.data.owner.owningKey, newOwner.owningKey)
    }

    private fun buildValidMoveTx(
        input: StateAndRef<TestContract.TestState>,
        newOwner: Party
    ): TransactionBuilder {
        return TransactionBuilder(input.state.notary)
            .addInputState(input)
            .addOutputState(input.state.data.copy(owner = newOwner), PROGRAM_ID)
            .addCommand(TestContract.Move(), input.state.data.owner.owningKey, newOwner.owningKey)
    }

    private fun runCreateTx(ownerNode: StartedMockNode, owner: Party): SignedTransaction {
        val tx = buildCreateTx(owner)
        val stx = ownerNode.services.signInitialTransaction(tx)
        val future = ownerNode.startFlow(
            ZKFinalityFlow(
                stx,
                emptyList(),
                zkConfig = loadZKConfig(ownerNode.services)

            )
        )
        mockNet.runNetwork()
        return future.getOrThrow()
    }

    private fun loadZKConfig(serviceHub: ServiceHub) = ZKConfig(
        proverService = serviceHub.getCordaServiceFromConfig("zkpProver"),
        verifierService = serviceHub.getCordaServiceFromConfig("zkpVerifier"),
        serializationFactoryService = serviceHub.getCordaServiceFromConfig("zkpSerializationFactoryService")
    )

    private fun buildCreateTx(owner: Party): TransactionBuilder {
        return TransactionBuilder(notary)
            .addOutputState(TestContract.TestState(owner), PROGRAM_ID)
            .addCommand(TestContract.Create(), owner.owningKey)
            .setTimeWindow(Instant.now(), Duration.ofSeconds(30))
    }
}
