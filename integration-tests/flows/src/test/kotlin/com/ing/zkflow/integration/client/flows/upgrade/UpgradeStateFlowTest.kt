package com.ing.zkflow.integration.client.flows.upgrade

import com.ing.zkflow.client.flows.ZKUpgradeStateFlow
import com.ing.zkflow.common.versioning.ContractStateVersionFamilyRegistry
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.node.services.InMemoryUtxoInfoStorage
import com.ing.zkflow.node.services.InMemoryZKVerifierTransactionStorageCordaService
import com.ing.zkflow.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zkflow.node.services.ServiceNames.ZK_UTXO_INFO_STORAGE
import com.ing.zkflow.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zkflow.notary.ZKNotaryService
import com.ing.zkflow.testing.checkIsPresentInVault
import com.ing.zkflow.testing.zkp.MockZKTransactionCordaService
import io.kotest.matchers.shouldBe
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

class UpgradeStateFlowTest {
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
    fun `End2End test with ZKP notary`() {
        val createFlow = CreateFlow(
            MyStateV1(miniCorp.anonymise()),
            CreateV1()
        )

        val createFuture = miniCorpNode.startFlow(createFlow)
        mockNet.runNetwork()
        val createStx = createFuture.getOrThrow()
        val oldState = createStx.tx.outRef<VersionedMyState>(0)
        miniCorpNode.checkIsPresentInVault(oldState, Vault.StateStatus.UNCONSUMED)

        val upgradeFlow = ZKUpgradeStateFlow(oldState, 2)
        val upgradeFuture = miniCorpNode.startFlow(upgradeFlow)
        mockNet.runNetwork()
        val upgradedState = upgradeFuture.getOrThrow()

        miniCorpNode.checkIsPresentInVault(oldState, Vault.StateStatus.CONSUMED)
        miniCorpNode.checkIsPresentInVault(upgradedState, Vault.StateStatus.UNCONSUMED)
        ContractStateVersionFamilyRegistry.versionOf(upgradedState.state.data::class) shouldBe 2
    }
}
