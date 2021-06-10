package com.ing.zknotary.common.client.flows

import com.ing.zknotary.common.client.flows.testflows.TestCollectZKSignaturesFlow
import com.ing.zknotary.node.services.ConfigParams.Zinc.COMMANDS_SEPARATOR
import com.ing.zknotary.node.services.ConfigParams.Zinc.COMMAND_CLASS_NAMES
import com.ing.zknotary.node.services.InMemoryUtxoInfoStorage
import com.ing.zknotary.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zknotary.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zknotary.node.services.ServiceNames.ZK_UTXO_INFO_STORAGE
import com.ing.zknotary.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zknotary.testing.fixtures.contract.TestContract
import com.ing.zknotary.testing.zkp.MockZKTransactionCordaService
import io.kotest.matchers.shouldBe
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
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
import org.junit.jupiter.api.Timeout

@Tag("slow")
class CollectSignaturesFlowTest {
    private val mockNet: MockNetwork
    private val notaryNode: StartedMockNode
    private val megaCorpNode: StartedMockNode
    private val miniCorpNode: StartedMockNode
    private val megaCorp: Party
    private val miniCorp: Party
    private val notary: Party

    init {
        val mockNetworkParameters = MockNetworkParameters(
            cordappsForAllNodes = listOf(
                cordappWithPackages("com.ing.zknotary").withConfig(
                    mapOf(
                        ZK_VERIFIER_TX_STORAGE to InMemoryZKVerifierTransactionStorage::class.qualifiedName!!,
                        ZK_UTXO_INFO_STORAGE to InMemoryUtxoInfoStorage::class.qualifiedName!!,
                        ZK_TX_SERVICE to MockZKTransactionCordaService::class.qualifiedName!!,
                        COMMAND_CLASS_NAMES to listOf(
                            TestContract.Create::class.java.name,
                            TestContract.Move::class.java.name
                        )
                            .joinToString(separator = COMMANDS_SEPARATOR)
                    )
                )
            ),
            notarySpecs = listOf(
                MockNetworkNotarySpec(DUMMY_NOTARY_NAME)
            )
        )
        mockNet = MockNetwork(mockNetworkParameters)
        notaryNode = mockNet.notaryNodes.first()
        megaCorpNode = mockNet.createPartyNode(CordaX500Name("MegaCorp", "London", "GB"))
        miniCorpNode = mockNet.createPartyNode(CordaX500Name("MiniCorp", "London", "GB"))
        notary = notaryNode.info.singleIdentity()
        megaCorp = megaCorpNode.info.singleIdentity()
        miniCorp = miniCorpNode.info.singleIdentity()
    }

    @AfterAll
    fun tearDown() {
        mockNet.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    @Test
    @Timeout(300)
    fun `Signing only on Initiator side`() {
        val p = TestCollectZKSignaturesFlow()
        val future = miniCorpNode.startFlow(p)
        mockNet.runNetwork()
        val svtx = future.getOrThrow()

        // Check signatures
        svtx.sigs.size shouldBe 1
        svtx.sigs.forEach {
            it.verify(svtx.id)
        }
    }

    @Test
    @Timeout(60)
    fun `Signing on two sides`() {

        // Move state
        val p = TestCollectZKSignaturesFlow(listOf(megaCorp))
        val future = miniCorpNode.startFlow(p)
        mockNet.runNetwork()
        val svtx = future.getOrThrow()

        // Check signatures
        svtx.sigs.size shouldBe 2
        svtx.sigs.forEach {
            it.verify(svtx.id)
        }
    }
}
