package com.example.rpc

import com.example.flow.IssuePrivateCBDCTokenFlow
import com.example.contract.cbdc.CBDCToken
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCClientConfiguration
import net.corda.client.rpc.CordaRPCConnection
import net.corda.client.rpc.RPCException
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowHandle
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.core.utilities.seconds
import org.apache.activemq.artemis.api.core.ActiveMQSecurityException
import java.time.Duration
import kotlin.concurrent.thread

class ZkdappTesterRpcClient(nodeUrlAndPort: String, val user: String, val password: String) {
    private val log = loggerFor<ZkdappTesterRpcClient>()

    private val nodeAddress = parse(nodeUrlAndPort)

    private val timeout: Duration = Duration.ofSeconds(60)

    private var rpcConnection: CordaRPCConnection? = null

    private var _proxy: CordaRPCOps? = null
    val proxy: CordaRPCOps
        get() = if (_proxy == null) throw IllegalStateException("RPCClient is not ready") else _proxy!!

    init {
        thread {
            rpcConnection = establishConnectionWithRetry(nodeAddress, user, password)
            _proxy = rpcConnection?.proxy
        }
    }

    fun create(token: CBDCToken): SignedTransaction {
        val flowHandle = proxyStartFlowDynamic(proxy, IssuePrivateCBDCTokenFlow::class.java, token)
        return flowHandle.returnValue.getOrThrow(timeout)
    }

    fun party(): Party = proxy.nodeInfo().legalIdentities.single()

    private fun establishConnectionWithRetry(
        nodeHostAndPort: NetworkHostAndPort,
        username: String,
        password: String
    ): CordaRPCConnection {
        val retryInterval = 5.seconds

        val client = CordaRPCClient(
            nodeHostAndPort,
            CordaRPCClientConfiguration.DEFAULT.copy(
                connectionMaxRetryInterval = retryInterval,
                trackRpcCallSites = true
            )
        )
        do {
            val connection = try {
                log.info("Connecting to: $nodeHostAndPort")
                val connection = client.start(username, password)
                // Check connection is truly operational before returning it.
                val nodeInfo = connection.proxy.nodeInfo()
                require(nodeInfo.legalIdentitiesAndCerts.isNotEmpty()) { "No identity certificates found" }
                connection
            } catch (exception: Exception) {
                // Deliberately not logging full stack trace as it will be full of internal stacktraces.
                log.warn("Exception upon establishing connection: ${exception.message}")
                null
            }

            if (connection != null) {
                log.info("Connection successfully established with: $nodeHostAndPort")
                return connection
            }
            // Could not connect this time round - pause before giving another try.
            Thread.sleep(retryInterval.toMillis())
        } while (connection == null)

        throw IllegalArgumentException("Never reaches here")
    }

    private fun performRPCReconnect() {
        thread {
            // flag to inform proxy is null when connection is lost
            _proxy = null

            log.warn("restarting rpc connection")

            // force closing the connection to avoid propagation of notification to the server side.
            rpcConnection?.forceClose()

            log.warn("removed previous invalid connection")
            rpcConnection = establishConnectionWithRetry(nodeAddress, user, password)
            _proxy = rpcConnection?.proxy
        }
    }

    // This function is a wrapper to run proxy.startFlowDynamic with exception handling
    private fun <T> proxyStartFlowDynamic(
        proxy: CordaRPCOps,
        logicType: Class<out FlowLogic<T>>,
        vararg args: Any?
    ): FlowHandle<T> {
        try {
            // Inside a function a vararg-parameter of type T is visible as an array of T.
            // use spread operator (*) to forward these
            return proxy.startFlowDynamic(logicType, *args)
        } catch (e: Exception) {
            when (e) {
                is RPCException, is ActiveMQSecurityException -> {
                    performRPCReconnect()
                    throw IllegalStateException("RPCClient connection is lost. Trying to reconnect")
                }
                else -> throw e
            }
        }
    }

    fun waitForConnection(waitingSeconds: Int = 20, errorMessage: String = "Couldn't connect to RPC ${nodeAddress.host + ":" + nodeAddress.port}") {
        val waitWebApplicationTimeout = System.currentTimeMillis() + waitingSeconds * 1000
        while (System.currentTimeMillis() < waitWebApplicationTimeout) {
            try {
                party()
                return
            } catch (e: Exception) {
                log.warn("", e)
                Thread.sleep(1000)
            }
        }
        error(errorMessage)
    }

//    // This function is a wrapper to run proxy.vaultQueryBy with exception handling
//    private inline fun <reified T : ContractState> proxyVaultQueryBy(
//        proxy: CordaRPCOps,
//        criteria: QueryCriteria = QueryCriteria.VaultQueryCriteria(),
//        paging: PageSpecification = PageSpecification(),
//        sorting: Sort = Sort(emptySet())
//    ): Vault.Page<T> {
//        try {
//            return proxy.vaultQueryBy(criteria, paging, sorting)
//        } catch (e: Exception) {
//            when (e) {
//                is RPCException, is ActiveMQSecurityException -> {
//                    performRPCReconnect()
//                    throw IllegalStateException("RPCClient is connection lost. Trying to reconnect")
//                }
//                else -> throw e
//            }
//        }
//    }
}
