package com.ing.zkflow.integration.zkp.metadata

import com.ing.zkflow.common.node.services.InMemoryUtxoInfoStorage
import com.ing.zkflow.common.node.services.InMemoryZKVerifierTransactionStorageCordaService
import com.ing.zkflow.common.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zkflow.common.node.services.ServiceNames.ZK_UTXO_INFO_STORAGE
import com.ing.zkflow.common.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.integration.client.flows.testflows.CreateFlow
import com.ing.zkflow.integration.client.flows.testflows.MoveFlow
import com.ing.zkflow.integration.contract.TestContract
import com.ing.zkflow.notary.ZKNotaryService
import com.ing.zkflow.testing.zkp.MockZKTransactionCordaService
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
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

class VisibilityFlowTest {
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
                        ZK_VERIFIER_TX_STORAGE to InMemoryZKVerifierTransactionStorageCordaService::class.qualifiedName!!,
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
    @Tag("slow")
    fun `Test enforce visibility`() {
        val createPublicFlow = CreateFlow(createCommand = TestContract.CreatePublic())
        val createPublicFuture = miniCorpNode.startFlow(createPublicFlow)
        mockNet.runNetwork()
        val createPublicStx = createPublicFuture.getOrThrow()

        val createPrivateFlow = CreateFlow(createCommand = TestContract.CreatePrivate()) // this command creates private output
        val createPrivateFuture = miniCorpNode.startFlow(createPrivateFlow)
        mockNet.runNetwork()
        val createPrivateStx = createPrivateFuture.getOrThrow()
        createPrivateStx.tx

        // MovePrivateOnly should fail for public asset, work for private asset
        try {
            val moveFuture = miniCorpNode.startFlow(MoveFlow(createPublicStx, megaCorp, moveCommand = TestContract.MovePrivateOnly()))
            mockNet.runNetwork()
            moveFuture.getOrThrow()
        } catch (fex: IllegalStateException) {
            assert(fex.message!!.contains("should be private, but it is public"))
        }

        val movePrivateOnlyFuture =
            miniCorpNode.startFlow(MoveFlow(createPrivateStx, megaCorp, moveCommand = TestContract.MovePrivateOnly()))
        mockNet.runNetwork()
        val movePrivateStx = movePrivateOnlyFuture.getOrThrow()

        // MoveAnyToPrivate should work for both

        val movePrivateFuture = miniCorpNode.startFlow(MoveFlow(movePrivateStx, megaCorp, moveCommand = TestContract.MoveAnyToPrivate()))
        mockNet.runNetwork()
        movePrivateFuture.getOrThrow()

        val movePublicFuture = miniCorpNode.startFlow(MoveFlow(createPublicStx, megaCorp, moveCommand = TestContract.MoveAnyToPrivate()))
        mockNet.runNetwork()
        movePublicFuture.getOrThrow()
    }
}
