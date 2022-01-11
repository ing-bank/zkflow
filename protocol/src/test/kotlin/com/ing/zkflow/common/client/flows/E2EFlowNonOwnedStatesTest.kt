package com.ing.zkflow.common.client.flows

import com.ing.zkflow.common.client.flows.testflows.CreateFlow
import com.ing.zkflow.common.client.flows.testflows.MoveBidirectionalFlow
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.node.services.InMemoryUtxoInfoStorage
import com.ing.zkflow.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zkflow.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zkflow.node.services.ServiceNames.ZK_UTXO_INFO_STORAGE
import com.ing.zkflow.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zkflow.notary.ZKNotaryService
import com.ing.zkflow.testing.fixtures.contract.TestContract
import com.ing.zkflow.testing.zkp.MockZKTransactionCordaService
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
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
                cordappWithPackages("com.ing.zkflow").withConfig(
                    mapOf(
                        ZK_VERIFIER_TX_STORAGE to InMemoryZKVerifierTransactionStorage::class.qualifiedName!!,
                        ZK_UTXO_INFO_STORAGE to InMemoryUtxoInfoStorage::class.qualifiedName!!,
                        ZK_TX_SERVICE to MockZKTransactionCordaService::class.qualifiedName!!,
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
            networkParameters = testNetworkParameters(minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION)
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
    fun `End2End test with ZKP notary - non-owned states`() {
        // Initiator creates a state they want to trade
        val createStxMiniCorpNode = createNewState(miniCorpNode)
        val miniCorpStateAndRef = createStxMiniCorpNode.coreTransaction.outRef<TestContract.TestState>(0)

        // Start the trade. We expect counterparty to trade a state of identical value
        val moveFuture = miniCorpNode.startFlow(
            MoveBidirectionalFlow(
                miniCorpStateAndRef,
                megaCorp
            )
        )
        mockNet.runNetwork()
        val moveStx = moveFuture.getOrThrow()

        checkVault(miniCorpNode, Vault.StateStatus.CONSUMED, miniCorpStateAndRef.ref)
        checkVault(
            miniCorpNode,
            Vault.StateStatus.UNCONSUMED,
            moveStx.tx.filterOutRefs<TestContract.TestState> { it.owner == miniCorp.anonymise() }.single().ref
        )
        checkVault(
            megaCorpNode,
            Vault.StateStatus.UNCONSUMED,
            moveStx.tx.filterOutRefs<TestContract.TestState> {
                it == miniCorpStateAndRef.state.data.withNewOwner(megaCorp.anonymise()).ownableState
            }.single().ref
        )
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
