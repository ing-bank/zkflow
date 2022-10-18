package com.example.flow

import com.example.contract.audit.AuditContract
import com.example.contract.cbdc.CBDCToken
import com.example.contract.cbdc.commands.IssuePrivate
import com.example.contract.cbdc.commands.MovePrivate
import com.example.contract.cbdc.commands.RedeemPrivate
import com.example.contract.cbdc.commands.SplitPrivate
import com.example.contract.cbdc.digitalEuro
import com.ing.zkflow.common.node.services.InMemoryZKVerifierTransactionStorageCordaService
import com.ing.zkflow.common.node.services.ServiceNames.ZK_TX_SERVICE
import com.ing.zkflow.common.node.services.ServiceNames.ZK_UTXO_INFO_STORAGE
import com.ing.zkflow.common.node.services.ServiceNames.ZK_VERIFIER_TX_STORAGE
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.ZKTransactionService
import com.ing.zkflow.common.node.services.InMemoryUtxoInfoStorage
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.zkp.zinc.ZincZKTransactionCordaService
import com.ing.zkflow.notary.ZKNotaryService
import com.ing.zkflow.testing.checkIsPresentInVault
import com.ing.zkflow.testing.checkIsPubliclyPresentInZKStorage
import com.ing.zkflow.testing.checkNotPresentInVault
import com.ing.zkflow.testing.checkNotPresentInZKStorage
import com.ing.zkflow.testing.zkp.MockZKTransactionCordaService
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

class PrivateCBDCTokenFlowTest {
    private val mockNet: MockNetwork

    private val notaryNode: StartedMockNode
    private val issuerNode: StartedMockNode
    private val aliceNode: StartedMockNode
    private val auditorNode: StartedMockNode
    private val bobNode: StartedMockNode

    private val notary: Party
    private val issuer: Party
    private val alice: AnonymousParty
    private val auditor: Party
    private val bob: AnonymousParty

    private val zkTransactionService: ZKTransactionService

    init {
        val mockNetworkParameters = MockNetworkParameters(
            cordappsForAllNodes = listOf(
                cordappWithPackages("com.example", "com.ing.zkflow").withConfig(
                    mapOf(
                        ZK_VERIFIER_TX_STORAGE to InMemoryZKVerifierTransactionStorageCordaService::class.qualifiedName!!,
                        ZK_UTXO_INFO_STORAGE to InMemoryUtxoInfoStorage::class.qualifiedName!!,
                        // ZK_TX_SERVICE to ZincZKTransactionCordaService::class.qualifiedName!!,
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
        issuerNode = mockNet.createPartyNode(CordaX500Name("Issuer", "London", "GB"))
        aliceNode = mockNet.createPartyNode(CordaX500Name("Alice", "London", "GB"))
        auditorNode = mockNet.createPartyNode(CordaX500Name("Auditor", "London", "GB"))
        bobNode = mockNet.createPartyNode(CordaX500Name("Bob", "London", "GB"))

        notary = notaryNode.info.singleIdentity()
        issuer = issuerNode.info.singleIdentity()
        alice = aliceNode.info.singleIdentity().anonymise()
        auditor = auditorNode.info.singleIdentity()
        bob = bobNode.info.singleIdentity().anonymise()

        aliceNode.registerInitiatedFlow(IssuePrivateCBDCTokenFlowFlowHandler::class.java)
        aliceNode.registerInitiatedFlow(SplitPrivateCBDCTokenFlowFlowHandler::class.java)
        bobNode.registerInitiatedFlow(MovePrivateCBDCTokenFlowFlowHandler::class.java)
        bobNode.registerInitiatedFlow(SplitPrivateCBDCTokenFlowFlowHandler::class.java)
        auditorNode.registerInitiatedFlow(MovePrivateCBDCTokenFlowFlowHandler::class.java)
        auditorNode.registerInitiatedFlow(SplitPrivateCBDCTokenFlowFlowHandler::class.java)

        // Because we are running in the context of a flow test, where all mock nodes share the same filesystem,
        // we only have to do setup once for all mock nodes to save time.
        // The other mock nodes will be able to access the circuit artifacts.
        zkTransactionService = aliceNode.services.getCordaServiceFromConfig(ZK_TX_SERVICE)

        // Do circuit setup. Outside of tests, this would be done as part of CorDapp deployment
        // and the artifacts would be distributed with the CorDapp.
        zkTransactionService.setup(IssuePrivate().metadata)
        zkTransactionService.setup(MovePrivate().metadata)
        zkTransactionService.setup(SplitPrivate().metadata)
        zkTransactionService.setup(RedeemPrivate().metadata)
    }

    @AfterAll
    fun tearDown() {
        mockNet.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    /*
     * TODO: Issue a V1 token, then move it, then upgrade it to V2, then redeem it
     */

    /*
     * TODO: Exchange UtxoInfo for an unknown private state exchange
     */

    @Test
    fun `Test private Issue, Move, Split and Redeem`() {
        // Issuer issues a private CBDCToken to Alice
        val issuePrivateCBDCTokenFlow = IssuePrivateCBDCTokenFlow(digitalEuro(1.00, issuer = issuer, holder = alice))
        val issuePrivateFuture = issuerNode.startFlow(issuePrivateCBDCTokenFlow)
        mockNet.runNetwork()
        val privateIssueStx = issuePrivateFuture.getOrThrow()

        // Confirm that Alice received the private token and can access it.
        val alicesIssuedToken = privateIssueStx.tx.outRef<CBDCToken>(0)
        aliceNode.checkIsPresentInVault(alicesIssuedToken, Vault.StateStatus.UNCONSUMED)

        // Alice moves the private token to Bob privately. It stays private.
        // Auditor receives the public contents of this transaction, but not the private
        val movePrivateCBDCTokenFlow = MovePrivateCBDCTokenFlow(alicesIssuedToken, bob, auditor)
        val movePrivateFuture = aliceNode.startFlow(movePrivateCBDCTokenFlow)
        mockNet.runNetwork()
        val privatemoveStx = movePrivateFuture.getOrThrow()

        /*
         * Confirm that:
         * - The token is consumed in Alice's vault.
         * - Bob received the token, and it is unconsumed.
         * - The auditor received the public contents of this transaction, but not the private
         */
        val bobsMovedToken = privatemoveStx.tx.outRef<CBDCToken>(0)
        val moveAuditRecord = privatemoveStx.tx.outRef<AuditContract.AuditRecord>(1)
        aliceNode.checkIsPresentInVault(alicesIssuedToken, Vault.StateStatus.CONSUMED)
        bobNode.checkIsPresentInVault(bobsMovedToken, Vault.StateStatus.UNCONSUMED)
        auditorNode.checkNotPresentInVault(bobsMovedToken)
        auditorNode.checkIsPubliclyPresentInZKStorage(moveAuditRecord)
        auditorNode.checkNotPresentInZKStorage(bobsMovedToken)

        // Bob moves a fraction of the private token to Alice privately and keeps the change.
        // Auditor receives the public contents of this transaction, but not the private
        val splitPrivateCBDCTokenFlow =
            SplitPrivateCBDCTokenFlow(bobsMovedToken, SplitInfo(OwnerWithQuantity(alice, 0.33), OwnerWithQuantity(bob, 0.67)), auditor)
        val splitPrivateFuture = bobNode.startFlow(splitPrivateCBDCTokenFlow)
        mockNet.runNetwork()
        val privatesplitStx = splitPrivateFuture.getOrThrow()

        /*
         * Confirm that:
         * - The token is consumed in Bob's vault.
         * - Alice received fraction of the token, and it is unconsumed.
         * - Bob received the change of the token, and it is unconsumed.
         * - The auditor received the public contents of this transaction, i.e. the audit record, but not the private contents.
         */
        val alicesFractionalToken = privatesplitStx.tx.outRef<CBDCToken>(0)
        val bobsChangeToken = privatesplitStx.tx.outRef<CBDCToken>(1)
        val splitAuditRecord = privatesplitStx.tx.outRef<AuditContract.AuditRecord>(2)
        bobNode.checkIsPresentInVault(bobsMovedToken, Vault.StateStatus.CONSUMED)
        aliceNode.checkIsPresentInVault(alicesFractionalToken, Vault.StateStatus.UNCONSUMED)
        bobNode.checkIsPresentInVault(bobsChangeToken, Vault.StateStatus.UNCONSUMED)
        auditorNode.checkNotPresentInVault(alicesFractionalToken)
        auditorNode.checkNotPresentInVault(bobsChangeToken)
        auditorNode.checkIsPubliclyPresentInZKStorage(splitAuditRecord)
        auditorNode.checkNotPresentInZKStorage(alicesFractionalToken)
        auditorNode.checkNotPresentInZKStorage(bobsChangeToken)

        // Bob redeems his change with the issuer
        val redeemCBDCTokenFlow = RedeemPrivateCBDCTokenFlow(bobsChangeToken)
        val redeemFuture = bobNode.startFlow(redeemCBDCTokenFlow)
        mockNet.runNetwork()
        redeemFuture.getOrThrow()

        /*
         * Confirm that:
         * - The token is consumed in Bob's vault.
         */
        bobNode.checkIsPresentInVault(bobsChangeToken, Vault.StateStatus.CONSUMED)
    }
}
