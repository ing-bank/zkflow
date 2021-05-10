package io.ivno.collateraltoken.workflow

import co.paralleluniverse.fibers.Suspendable
import io.onixlabs.corda.core.workflow.firstNotary
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub

/**
 * Gets the preferred notary from the node config, or alternatively a default notary in the event that
 * a preferred notary has not been specified in the node config.
 *
 * @param defaultSelector The selector function to obtain a notary if none have been specified in the node config.
 * @return Returns the preferred or default notary.
 * @throws IllegalAccessException If the preferred notary cannot be found in the network map cache.
 */
@Suspendable
fun FlowLogic<*>.getPreferredNotary(defaultSelector: (ServiceHub) -> Party = { firstNotary }): Party {
    return if (serviceHub.getAppContext().config.exists("notary")) {
        val name = CordaX500Name.parse(serviceHub.getAppContext().config.getString("notary"))
        serviceHub.networkMapCache.getNotary(name) ?: throw IllegalArgumentException(
            "Notary with the specified name cannot be found in the network map cache: $name."
        )
    } else {
        defaultSelector(serviceHub)
    }
}
