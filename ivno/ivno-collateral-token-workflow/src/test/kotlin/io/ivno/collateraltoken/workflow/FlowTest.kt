package io.ivno.collateraltoken.workflow

import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.*
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.Role
import io.onixlabs.corda.bnms.contract.Setting
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import io.onixlabs.corda.bnms.contract.membership.accept
import io.onixlabs.corda.bnms.workflow.membership.IssueMembershipAttestationFlow
import io.onixlabs.corda.bnms.workflow.membership.IssueMembershipFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

typealias MembershipAndAttestation = Pair<StateAndRef<Membership>, StateAndRef<MembershipAttestation>>

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class FlowTest {

    val NETWORK by lazy { Network("Ivno", bnoParty) }

    val TOKEN_TYPE by lazy { IvnoTokenType(NETWORK, custodianParty, tieParty, "GBP", 2) }
    val TOKEN_TYPE_POINTER by lazy { TOKEN_TYPE.toPointer() }
    val TOKEN_TYPE_OBSERVERS by lazy { setOf(bankPartyA, bankPartyB, custodianParty) }

    val TOKEN_OF_100GBP by lazy { BigDecimalAmount(100, TOKEN_TYPE_POINTER) }
    val TOKEN_OF_50GBP by lazy { BigDecimalAmount(50, TOKEN_TYPE_POINTER) }
    val TOKEN_OF_30GBP by lazy { BigDecimalAmount(30, TOKEN_TYPE_POINTER) }
    val TOKEN_OF_10GBP by lazy { BigDecimalAmount(10, TOKEN_TYPE_POINTER) }

    val DEPOSIT by lazy { Deposit(bankPartyA, custodianParty, tieParty, TOKEN_OF_100GBP, "12345678") }
    val TRANSFER_REQUEST by lazy {
        Transfer(
            bankPartyA,
            bankPartyB,
            TransferInitiator.TARGET_HOLDER,
            TOKEN_OF_50GBP,
            "12345678",
            "87654321"
        )
    }
    val TRANSFER_SEND by lazy {
        Transfer(
            bankPartyA,
            bankPartyB,
            TransferInitiator.CURRENT_HOLDER,
            TOKEN_OF_50GBP,
            "12345678",
            "87654321"
        )
    }

    val REDEMPTION by lazy { Redemption(bankPartyB, custodianParty, tieParty, TOKEN_OF_30GBP, "87654321") }

    private lateinit var _network: MockNetwork

    @PublishedApi
    internal val network: MockNetwork
        get() = _network

    private lateinit var _notaryNode: StartedMockNode
    protected val notaryNode: StartedMockNode get() = _notaryNode
    private lateinit var _notaryParty: Party
    protected val notaryParty: Party get() = _notaryParty

    private lateinit var _bankNodeA: StartedMockNode
    protected val bankNodeA: StartedMockNode get() = _bankNodeA
    private lateinit var _bankPartyA: Party
    protected val bankPartyA: Party get() = _bankPartyA

    private lateinit var _bankNodeB: StartedMockNode
    protected val bankNodeB: StartedMockNode get() = _bankNodeB
    private lateinit var _bankPartyB: Party
    protected val bankPartyB: Party get() = _bankPartyB

    private lateinit var _tieNode: StartedMockNode
    protected val tieNode: StartedMockNode get() = _tieNode
    private lateinit var _tieParty: Party
    protected val tieParty: Party get() = _tieParty

    private lateinit var _custodianNode: StartedMockNode
    protected val custodianNode: StartedMockNode get() = _custodianNode
    private lateinit var _custodianParty: Party
    protected val custodianParty: Party get() = _custodianParty

    private lateinit var _bnoNode: StartedMockNode
    protected val bnoNode: StartedMockNode get() = _bnoNode
    private lateinit var _bnoParty: Party
    protected val bnoParty: Party get() = _bnoParty

    protected open fun initialize() = Unit
    protected open fun finalize() = Unit

    protected fun setupBusinessNetwork() {
        bnoNode.createAttestedMembership()
        custodianNode.createAttestedMembership()
        tieNode.createAttestedMembership(setOf(Role("TOKEN_ISSUING_ENTITY")))
        bankNodeA.createAttestedMembership(setOf(Setting("TOKEN_OBSERVER", bnoParty.toString())))
        bankNodeB.createAttestedMembership(setOf(Setting("TOKEN_OBSERVER", bnoParty.toString())))
    }

    protected fun StartedMockNode.createAttestedMembership(settings: Set<Setting<*>> = emptySet()): MembershipAndAttestation {
        lateinit var issuedMembership: StateAndRef<Membership>
        lateinit var issuedAttestation: StateAndRef<MembershipAttestation>

        Pipeline
            .create(network)
            .run(this) {
                val membership = Membership(NETWORK, info.singleIdentity(), settings = settings)
                IssueMembershipFlow.Initiator(membership)
            }
            .run(bnoNode) {
                issuedMembership = it.tx.outRefsOfType<Membership>().single()
                val attestation = issuedMembership.accept(bnoParty)
                IssueMembershipAttestationFlow.Initiator(attestation)
            }
            .finally {
                issuedAttestation = it.tx.outRefsOfType<MembershipAttestation>().single()
            }

        return issuedMembership to issuedAttestation
    }

    @BeforeAll
    private fun setup() {

        val config: Map<String, String> = mapOf("notary" to "O=Notary Service, L=Zurich, C=CH")
        _network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("io.onixlabs.corda.identityframework.contract"),
                    TestCordapp.findCordapp("io.onixlabs.corda.identityframework.workflow"),
                    TestCordapp.findCordapp("io.onixlabs.corda.bnms.contract"),
                    TestCordapp.findCordapp("io.onixlabs.corda.bnms.workflow"),
                    TestCordapp.findCordapp("io.ivno.collateraltoken.contract"),
                    TestCordapp.findCordapp("io.ivno.collateraltoken.services"),
                    TestCordapp.findCordapp("io.ivno.collateraltoken.workflow").withConfig(config),
                    TestCordapp.findCordapp("io.dasl.contracts.v1"),
                    TestCordapp.findCordapp("io.dasl.workflows")
                ),
                networkParameters = testNetworkParameters(
                    minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION
                )
            )
        )

        _notaryNode = network.defaultNotaryNode
        _bankNodeA = network.createPartyNode(CordaX500Name("Bank A", "London", "GB"))
        _bankNodeB = network.createPartyNode(CordaX500Name("Bank B", "London", "GB"))
        _tieNode = network.createPartyNode(CordaX500Name("Token Issuing Entity", "Paris", "FR"))
        _custodianNode = network.createPartyNode(CordaX500Name("Custodian", "New York", "US"))
        _bnoNode = network.createPartyNode(CordaX500Name("BNO", "London", "GB"))

        _notaryParty = notaryNode.info.singleIdentity()
        _bankPartyA = bankNodeA.info.singleIdentity()
        _bankPartyB = bankNodeB.info.singleIdentity()
        _tieParty = tieNode.info.singleIdentity()
        _custodianParty = custodianNode.info.singleIdentity()
        _bnoParty = bnoNode.info.singleIdentity()

        setupBusinessNetwork()
        initialize()
    }

    @AfterAll
    private fun tearDown() {
        network.stopNodes()
        finalize()
    }
}
