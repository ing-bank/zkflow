package com.ing.zknotary.common.flows

import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SerializeAsToken

/**
 * Returns a singleton of type [T].
 *
 * This function throws an exception if:
 * - the config key does not exist in the CorDapp
 * - the config value refers to a class that doesn't exist. (Note that it should be a fully qualified name).
 * - the class is not a properly registered [CordaService]
 */
fun <T : SerializeAsToken> ServiceHub.getCordaServiceFromConfig(configKey: String): T {
    val config = this.getAppContext().config

    @Suppress("UNCHECKED_CAST")
    val clazz = Class.forName(config.getString(configKey)) as Class<T>

    return this.cordaService(clazz)
}