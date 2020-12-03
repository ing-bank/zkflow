package com.ing.zknotary.common.client.flows

import com.ing.zknotary.node.services.InMemoryZKProverTransactionStorage
import com.ing.zknotary.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zknotary.node.services.ServiceNames.ZK_PROVER_TX_STORAGE
import com.ing.zknotary.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zknotary.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zknotary.notary.ZKNotaryService
import com.ing.zknotary.testing.zkp.MockZKTransactionService
import junit.framework.TestCase.assertEquals
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
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotarisationFlowTest {
    private lateinit var mockNet: MockNetwork
    private lateinit var notaryNode: StartedMockNode
    private lateinit var megaCorpNode: StartedMockNode
    private lateinit var miniCorpNode: StartedMockNode
    private lateinit var megaCorp: Party
    private lateinit var miniCorp: Party
    private lateinit var notary: Party

    @Before
    fun setup() {
        val mockNetworkParameters = MockNetworkParameters(
            cordappsForAllNodes = listOf(
                cordappWithPackages("com.ing.zknotary").withConfig(
                    mapOf(
                        ZK_PROVER_TX_STORAGE to InMemoryZKProverTransactionStorage::class.qualifiedName!!,
                        ZK_VERIFIER_TX_STORAGE to InMemoryZKVerifierTransactionStorage::class.qualifiedName!!,
                        ZK_TX_SERVICE to MockZKTransactionService::class.qualifiedName!!
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

    @After
    fun tearDown() {
        mockNet.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    @Test(timeout = 30_000)
    fun `Notary signing`() {
        val createFlow = TestNotarisationFlow()
        val future = miniCorpNode.startFlow(createFlow)
        mockNet.runNetwork()
        val signedTxs = future.getOrThrow()

        // Check normal Tx signatures
        assertEquals(1, signedTxs.first.sigs.size)
        signedTxs.first.sigs.forEach {
            it.isValid(signedTxs.first.id)
        }

        // Check ZKP Tx signatures
        assertEquals(2, signedTxs.second.sigs.size)
        signedTxs.second.sigs.forEach {
            it.isValid(signedTxs.second.id)
        }

        // Check that notary sig is here
        assert(
            signedTxs.second.sigs.filter {
                it.by == notaryNode.info.legalIdentities.single().owningKey
            }.size == 1
        )
    }
}
