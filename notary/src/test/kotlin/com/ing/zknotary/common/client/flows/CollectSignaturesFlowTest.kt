package com.ing.zknotary.common.client.flows

import groovy.util.GroovyTestCase.assertEquals
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
import org.junit.After
import org.junit.Before
import org.junit.Test

class CollectSignaturesFlowTest {
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
            cordappsForAllNodes = listOf(cordappWithPackages("com.ing.zknotary")),
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

    @After
    fun tearDown() {
        mockNet.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    @Test(timeout = 300_000)
    fun `Signing only on Initiator side`() {
        val p = TestCollectSignaturesForCreateFlow()
        val future = miniCorpNode.startFlow(p)
        mockNet.runNetwork()
        val signedTxs = future.getOrThrow()

        // Check normal Tx signatures
        assertEquals(1, signedTxs.first.sigs.size)
        signedTxs.first.sigs.forEach {
            it.isValid(signedTxs.first.id)
        }

        // Check ZKP Tx signatures
        assertEquals(1, signedTxs.second.sigs.size)
        signedTxs.second.sigs.forEach {
            it.isValid(signedTxs.second.id)
        }
    }

    @Test(timeout = 300_000)
    fun `Signing on two sides`() {

        // Move state
        val p = TestCollectSignaturesForCreateFlow(listOf(megaCorp))
        val future = miniCorpNode.startFlow(p)
        mockNet.runNetwork()
        val signedTxs = future.getOrThrow()

        // Check normal Tx signatures
        assertEquals(2, signedTxs.first.sigs.size)
        signedTxs.first.sigs.forEach {
            it.isValid(signedTxs.first.id)
        }

        // Check ZKP Tx signatures
        assertEquals(2, signedTxs.second.sigs.size)
        signedTxs.second.sigs.forEach {
            it.isValid(signedTxs.second.id)
        }
    }
}
