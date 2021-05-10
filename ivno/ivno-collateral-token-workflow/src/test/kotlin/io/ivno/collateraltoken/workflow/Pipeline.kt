package io.ivno.collateraltoken.workflow

import net.corda.core.flows.FlowLogic
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import java.time.Duration

class Pipeline<T>(val result: T, val network: MockNetwork, val timeout: Duration) {

    companion object {
        fun create(network: MockNetwork, duration: Duration = Duration.ofSeconds(30)): Pipeline<Any?> {
            return Pipeline(null, network, duration)
        }
    }

    fun <U> run(node: StartedMockNode, action: (T) -> FlowLogic<U>): Pipeline<U> {
        val future = node.startFlow(action(result))
        network.runNetwork()
        return Pipeline(future.getOrThrow(timeout), network, timeout)
    }

    fun finally(action: (T) -> Unit) {
        action(result)
    }
}
