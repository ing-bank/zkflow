package io.ivno.collateraltoken.integration

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.PortAllocation
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import net.corda.testing.node.internal.cordappsForPackages

abstract class IntegrationTest {

    companion object {
        val TOKEN_ISSUING_ENTITY_NAME = CordaX500Name("Token Issuing Entity", "London", "GB")
        val CUSTODIAN_NAME = CordaX500Name("Custodian", "London", "GB")
        val BANK_A_NAME = CordaX500Name("Bank A", "London", "GB")
        val BANK_B_NAME = CordaX500Name("Bank B", "New York", "US")
        val BANK_C_NAME = CordaX500Name("Bank C", "Tokyo", "JP")
        val NETWORK_OPERATOR = CordaX500Name("Network Operator", "London", "GB")

        private val logger = loggerFor<IntegrationTest>()
    }

    private lateinit var _tokenIssuingEntity: NodeHandle
    private lateinit var _custodianNode: NodeHandle
    private lateinit var _bankNodeA: NodeHandle
    private lateinit var _bankNodeB: NodeHandle
    private lateinit var _bankNodeC: NodeHandle
    private lateinit var _networkOperator: NodeHandle

    protected val tokenIssuingEntityNode: NodeHandle get() = _tokenIssuingEntity
    protected val custodianNode: NodeHandle get() = _custodianNode
    protected val bankNodeA: NodeHandle get() = _bankNodeA
    protected val bankNodeB: NodeHandle get() = _bankNodeB
    protected val bankNodeC: NodeHandle get() = _bankNodeC
    protected val networkOperator: NodeHandle get() = _networkOperator

    fun start(action: () -> Unit) {
        val rpcUsers = listOf(User("guest", "letmein", setOf("ALL")))

        val parameters = DriverParameters(
            isDebug = true,
            startNodesInProcess = true,
            waitForAllNodesToFinish = true,
            cordappsForAllNodes = cordappsForPackages(
                "io.onixlabs.corda.identityframework.contract",
                "io.onixlabs.corda.identityframework.workflow",
                "io.onixlabs.corda.bnms.contract",
                "io.onixlabs.corda.bnms.workflow",
                "io.ivno.collateraltoken.contract",
                "io.ivno.collateraltoken.services",
                "io.ivno.collateraltoken.workflow",
                "io.dasl.contracts.v1",
                "io.dasl.workflows"
            ),
            portAllocation = object : PortAllocation() {
                private var port: Int = 10000
                override fun nextPort(): Int = port++
            },
            networkParameters = testNetworkParameters(
                minimumPlatformVersion = 8
            )
        )

        driver(parameters) {
            _tokenIssuingEntity = startNode(providedName = TOKEN_ISSUING_ENTITY_NAME, rpcUsers = rpcUsers).getOrThrow()
            _custodianNode = startNode(providedName = CUSTODIAN_NAME, rpcUsers = rpcUsers).getOrThrow()
            _bankNodeA = startNode(providedName = BANK_A_NAME, rpcUsers = rpcUsers).getOrThrow()
            _bankNodeB = startNode(providedName = BANK_B_NAME, rpcUsers = rpcUsers).getOrThrow()
            _bankNodeC = startNode(providedName = BANK_C_NAME, rpcUsers = rpcUsers).getOrThrow()
            _networkOperator = startNode(providedName = NETWORK_OPERATOR, rpcUsers = rpcUsers).getOrThrow()

            listOf(_tokenIssuingEntity, _custodianNode, _bankNodeA, _bankNodeB, _bankNodeC, _networkOperator).forEach {
                val identity = it.nodeInfo.legalIdentities.first()
                val rpcAddress = it.rpcAddress
                logger.info("Node registered. RPC: '$rpcAddress' for node '$identity'.")
            }
        }

        initialize()
        action()
        finalize()
    }

    protected open fun initialize() = Unit

    protected open fun finalize() = Unit
}
