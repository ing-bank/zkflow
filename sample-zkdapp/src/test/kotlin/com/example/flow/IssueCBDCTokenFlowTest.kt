package com.example.flow

import com.example.contract.cbdc.CBDCContract
import com.example.token.cbdc.CBDCToken
import com.example.token.cbdc.digitalEuro
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.ZincZKTransactionCordaService
import com.ing.zkflow.node.services.InMemoryUtxoInfoStorage
import com.ing.zkflow.node.services.InMemoryZKVerifierTransactionStorageCordaService
import com.ing.zkflow.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zkflow.node.services.ServiceNames.ZK_UTXO_INFO_STORAGE
import com.ing.zkflow.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zkflow.notary.ZKNotaryService
import com.ing.zkflow.testing.checkVault
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
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

class IssueCBDCTokenFlowTest {
    private val mockNet: MockNetwork
    private val notaryNode: StartedMockNode
    private val issuerNode: StartedMockNode
    private val aliceNode: StartedMockNode
    private val issuer: Party
    private val alice: AnonymousParty
    private val notary: Party

    private val zkTransactionService: ZincZKTransactionCordaService

    init {
        val mockNetworkParameters = MockNetworkParameters(
            cordappsForAllNodes = listOf(
                cordappWithPackages("com.example", "com.ing.zkflow").withConfig(
                    mapOf(
                        ZK_VERIFIER_TX_STORAGE to InMemoryZKVerifierTransactionStorageCordaService::class.qualifiedName!!,
                        ZK_UTXO_INFO_STORAGE to InMemoryUtxoInfoStorage::class.qualifiedName!!,
                        ZK_TX_SERVICE to ZincZKTransactionCordaService::class.qualifiedName!!,
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
        issuerNode = mockNet.createPartyNode(CordaX500Name("Issuer", "London", "GB"))
        aliceNode = mockNet.createPartyNode(CordaX500Name("Alice", "London", "GB"))
        notary = notaryNode.info.singleIdentity()
        issuer = issuerNode.info.singleIdentity()
        alice = aliceNode.info.singleIdentity().anonymise()

        aliceNode.registerInitiatedFlow(IssuePrivateCBDCTokenFlowFlowHandler::class.java)

        zkTransactionService = aliceNode.services.cordaService(ZincZKTransactionCordaService::class.java)

        // DO CIRCUIT SETUP
        zkTransactionService.setup(CBDCContract.IssuePrivate().metadata, force = true)
    }

    @AfterAll
    fun tearDown() {
        mockNet.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    @Test
    fun `Test private issuance`() {
        val issuePrivateCBDCTokenFlow = IssuePrivateCBDCTokenFlow(digitalEuro(1.00, issuer = issuer, holder = alice))
        val issuePrivateFuture = issuerNode.startFlow(issuePrivateCBDCTokenFlow)
        mockNet.runNetwork()
        val privateIssueStx = issuePrivateFuture.getOrThrow()

        val expectedInVault = privateIssueStx.tx.outRef<CBDCToken>(0)
        checkVault(aliceNode, expectedInVault, Vault.StateStatus.UNCONSUMED)

        zkTransactionService.verify(zkTransactionService.vtxStorage.getTransaction(privateIssueStx.id)!!.tx)
    }
}
