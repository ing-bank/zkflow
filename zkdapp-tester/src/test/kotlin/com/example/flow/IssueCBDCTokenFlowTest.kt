package com.example.flow

import com.example.contract.CBDCContract
import com.example.contract.MockAssetContract
import com.example.token.cbdc.CBDCToken
import com.example.token.cbdc.IssuedTokenType
import com.example.token.cbdc.TokenType
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.ZincZKTransactionCordaService
import com.ing.zkflow.node.services.InMemoryUtxoInfoStorage
import com.ing.zkflow.node.services.InMemoryZKVerifierTransactionStorageCordaService
import com.ing.zkflow.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zkflow.node.services.ServiceNames.ZK_UTXO_INFO_STORAGE
import com.ing.zkflow.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zkflow.notary.ZKNotaryService
import com.ing.zkflow.testing.checkVault
import net.corda.core.contracts.Amount
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
import java.math.BigDecimal

class IssueCBDCTokenFlowTest {
    private val mockNet: MockNetwork
    private val notaryNode: StartedMockNode
    private val megaCorpNode: StartedMockNode
    private val miniCorpNode: StartedMockNode
    private val thirdPartyNode: StartedMockNode
    private val megaCorp: Party
    private val miniCorp: Party
    private val thirdParty: Party
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
        megaCorpNode = mockNet.createPartyNode(CordaX500Name("MegaCorp", "London", "GB"))
        miniCorpNode = mockNet.createPartyNode(CordaX500Name("MiniCorp", "London", "GB"))
        thirdPartyNode = mockNet.createPartyNode(CordaX500Name("ThirdParty", "London", "GB"))
        notary = notaryNode.info.singleIdentity()
        megaCorp = megaCorpNode.info.singleIdentity()
        miniCorp = miniCorpNode.info.singleIdentity()
        thirdParty = thirdPartyNode.info.singleIdentity()

        zkTransactionService = miniCorpNode.services.cordaService(ZincZKTransactionCordaService::class.java)

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
        val issuedTokenType = IssuedTokenType(megaCorp.anonymise(), TokenType("test-token", 2))
        val issuePrivateCBDCTokenFlow = IssuePrivateCBDCTokenFlow(Amount.fromDecimal(BigDecimal.ONE, issuedTokenType), miniCorp.anonymise())
        val issuePrivateFuture = miniCorpNode.startFlow(issuePrivateCBDCTokenFlow)
        mockNet.runNetwork()
        val privateIssueStx = issuePrivateFuture.getOrThrow()

        val expectedInVault = privateIssueStx.tx.outRef<CBDCToken>(0)
        checkVault(miniCorpNode, expectedInVault, Vault.StateStatus.UNCONSUMED)

        zkTransactionService.verify(zkTransactionService.vtxStorage.getTransaction(privateIssueStx.id)!!.tx)
    }
}
