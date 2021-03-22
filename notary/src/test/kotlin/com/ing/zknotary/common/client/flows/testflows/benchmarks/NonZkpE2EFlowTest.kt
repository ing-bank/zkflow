package com.ing.zknotary.common.client.flows.testflows.benchmarks

import com.ing.zknotary.common.client.flows.checkVault
import com.ing.zknotary.node.services.ConfigParams
import com.ing.zknotary.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zknotary.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zknotary.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zknotary.testing.fixtures.contract.TestContract
import com.ing.zknotary.testing.zkp.MockZKTransactionService
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

class NonZkpE2EFlowTest {
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
                            TestContract.Move::class.java.name
                        )
                            .joinToString(separator = ConfigParams.Zinc.COMMANDS_SEPARATOR)
                    )
                )
            ),
            notarySpecs = listOf(
                MockNetworkNotarySpec(
                    DUMMY_NOTARY_NAME,
                    validating = true
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
    fun `End2End test with normal notary as benchmark`() {
        val createFlow = NonZkpCreateFlow()
        val createFuture = miniCorpNode.startFlow(createFlow)
        mockNet.runNetwork()
        val createStx = createFuture.getOrThrow()

        checkVault(createStx, null, miniCorpNode)

        val moveFuture = miniCorpNode.startFlow(NonZkpMoveFlow(createStx, megaCorp))
        mockNet.runNetwork()
        val moveStx = moveFuture.getOrThrow()

        checkVault(moveStx, miniCorpNode, megaCorpNode)

        val moveBackFuture = megaCorpNode.startFlow(NonZkpMoveFlow(moveStx, miniCorp))
        mockNet.runNetwork()
        val moveBackStx = moveBackFuture.getOrThrow()

        checkVault(moveBackStx, megaCorpNode, miniCorpNode)

        val finalMoveFuture = miniCorpNode.startFlow(NonZkpMoveFlow(moveBackStx, thirdParty))
        mockNet.runNetwork()
        val finalTx = finalMoveFuture.getOrThrow()

        checkVault(finalTx, miniCorpNode, thirdPartyNode)
    }
}
