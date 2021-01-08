package com.ing.zknotary.common.client.flows

import com.ing.zknotary.common.client.flows.testflows.NotarisationFlow
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.node.services.ConfigParams
import com.ing.zknotary.node.services.InMemoryZKProverTransactionStorage
import com.ing.zknotary.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zknotary.node.services.ServiceNames.ZK_PROVER_TX_STORAGE
import com.ing.zknotary.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zknotary.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zknotary.notary.ZKNotaryService
import com.ing.zknotary.testing.zkp.MockZKTransactionService
import io.kotest.matchers.shouldBe
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
import org.junit.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Timeout

class NotarisationFlowTest {
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
                        ZK_PROVER_TX_STORAGE to InMemoryZKProverTransactionStorage::class.qualifiedName!!,
                        ZK_VERIFIER_TX_STORAGE to InMemoryZKVerifierTransactionStorage::class.qualifiedName!!,
                        ZK_TX_SERVICE to MockZKTransactionService::class.qualifiedName!!,
                        ConfigParams.Zinc.COMMAND_CLASS_NAMES to listOf(TestContract.Create::class.java.name, TestContract.Move::class.java.name)
                            .joinToString(separator = ConfigParams.Zinc.COMMANDS_SEPARATOR)
                    )
                )
            ),
            notarySpecs = listOf(
                MockNetworkNotarySpec(DUMMY_NOTARY_NAME, validating = false, className = ZKNotaryService::class.java.name)
            ),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 6)
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
    @Timeout(30)
    fun `Notary signing`() {
        val createFlow = NotarisationFlow()
        val future = miniCorpNode.startFlow(createFlow)
        mockNet.runNetwork()
        val signedTxs = future.getOrThrow()

        // Check normal Tx signatures
        signedTxs.first.sigs.size shouldBe 1
        signedTxs.first.sigs.forEach {
            it.verify(signedTxs.first.id)
        }

        // Check ZKP Tx signatures
        signedTxs.second.sigs.size shouldBe 2
        signedTxs.second.sigs.forEach {
            it.verify(signedTxs.second.id)
        }

        // Check that notary sig is here

        signedTxs.second.sigs.filter {
            it.by == notaryNode.info.legalIdentities.single().owningKey
        }.size shouldBe 1
    }
}
