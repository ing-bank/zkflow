package com.ing.zknotary.common.client.flows

import com.ing.zknotary.common.client.flows.testflows.CreateFlow
import com.ing.zknotary.common.client.flows.testflows.MoveBidirectionalFlow
import com.ing.zknotary.node.services.ConfigParams
import com.ing.zknotary.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zknotary.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zknotary.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zknotary.notary.ZKNotaryService
import com.ing.zknotary.testing.fixtures.contract.TestContract
import com.ing.zknotary.testing.zkp.MockZKTransactionService
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.internal.cordappWithPackages
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class E2EFlowNonOwnedStatesTest {
    private val mockNet: MockNetwork
    private val notaryNode: StartedMockNode
    private val megaCorpNode: StartedMockNode
    private val miniCorpNode: StartedMockNode
    private val thirdPartyNode: StartedMockNode
    private val megaCorp: Party
    private val miniCorp: Party
    private val thirdParty: Party
    private val notary: Party

    init {
        val mockNetworkParameters = MockNetworkParameters(
            cordappsForAllNodes = listOf(
                cordappWithPackages("com.ing.zknotary").withConfig(
                    mapOf(
                        ZK_VERIFIER_TX_STORAGE to InMemoryZKVerifierTransactionStorage::class.qualifiedName!!,
                        ZK_TX_SERVICE to MockZKTransactionService::class.qualifiedName!!,
                        ConfigParams.Zinc.COMMAND_CLASS_NAMES to listOf(
                            TestContract.Create::class.java.name,
                            TestContract.MoveBidirectional::class.java.name
                        ).joinToString(separator = ConfigParams.Zinc.COMMANDS_SEPARATOR)
                    )
                )
            ),
            notarySpecs = listOf(
                MockNetworkNotarySpec(
                    DUMMY_NOTARY_NAME,
                    validating = false,
                    className = ZKNotaryService::class.java.name
                )
            ),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 6)
        )
        mockNet = MockNetwork(mockNetworkParameters)
        notaryNode = mockNet.notaryNodes.first()
        megaCorpNode = mockNet.createPartyNode(CordaX500Name("MegaCorp", "London", "GB"))
        miniCorpNode = mockNet.createPartyNode(CordaX500Name("MiniCorp", "London", "GB"))
        thirdPartyNode = mockNet.createPartyNode(CordaX500Name("ThirdParty", "London", "GB"))
        notary = notaryNode.info.singleIdentity()
        megaCorp = megaCorpNode.info.singleIdentity()
        miniCorp = miniCorpNode.info.singleIdentity()
        thirdParty = thirdPartyNode.info.singleIdentity()
    }

    @AfterAll
    fun tearDown() {
        mockNet.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    @Test
    @Tag("slow")
    fun `End2End test with ZKP notary - non-owned states`() {
        // Both parties create a state
        val createStxMiniCorpNode = createNewState(miniCorpNode)
//        val createStxMegaCorpnode = createNewState(megaCorpNode)

        /**
         * MiniCorp selects the StateAndRef they want to sell
         */
        val miniCorpStateAndRef = createStxMiniCorpNode.coreTransaction.outRef<TestContract.TestState>(0)

//        /**
//         * MegaCorp provides to MiniCorp for the state they want to 'sell':
//         * - StateRef,
//         * - serialized contents of the UTXO that StateRef points to
//         * - the nonce for the the UTXO that StateRef points to
//         */
//        val megaCorpstateAndRef = createStxMegaCorpnode.tx.outRef<TestContract.TestState>(0)
//        val (serializedStates, nonces) = megaCorpNode.services.collectSerializedUtxosAndNonces(listOf(megaCorpstateAndRef.ref))

        val moveFuture = miniCorpNode.startFlow(
            MoveBidirectionalFlow(
                miniCorpStateAndRef,
                megaCorp
            )
        )
        mockNet.runNetwork()
        val moveStx = moveFuture.getOrThrow()

//        checkVault(moveStx, miniCorpNode, megaCorpNode)
    }

    private fun createNewState(owner: StartedMockNode): SignedTransaction {
        val createFlow = CreateFlow()
        val createFuture = owner.startFlow(createFlow)
        mockNet.runNetwork()
        val createStx = createFuture.getOrThrow()

        checkVault(createStx, null, owner)
        return createStx
    }
}
