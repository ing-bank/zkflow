package com.ing.zkflow.node.services

import net.corda.core.cordapp.CordappConfigException
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SerializeAsToken

/**
 * Returns a singleton of type [T].
 *
 * This function throws an exception if:
 * - the config key does not exist in the CorDapp
 * - the config value refers to a class that can't be found on the classpath. (Note that it should be a fully qualified name).
 * - the class is not a properly registered Corda service: It should be annotated with @CordaService
 */
fun <T : SerializeAsToken> ServiceHub.getCordaServiceFromConfig(configKey: String): T {
    val serviceClassName = this.getAppContext().config.getString(configKey)

    @Suppress("UNCHECKED_CAST")
    val clazz = Class.forName(serviceClassName) as Class<T>

    return this.cordaService(clazz)
}

object ServiceNames {
    const val ZK_TX_SERVICE: String = "zkTxService"
    const val ZK_VERIFIER_TX_STORAGE: String = "zkVerifierTxStorage"
    const val ZK_UTXO_INFO_STORAGE: String = "utxoInfoStorage"
}

fun ServiceHub.getStringFromConfig(configKey: String, defaultValue: String? = null): String {
    return try {
        this.getAppContext().config.getString(configKey)
    } catch (ex: CordappConfigException) {
        defaultValue ?: throw ex
    }
}

fun ServiceHub.getLongFromConfig(configKey: String, defaultValue: Long? = null): Long {
    return try {
        this.getAppContext().config.getLong(configKey)
    } catch (ex: CordappConfigException) {
        defaultValue ?: throw ex
    }
}
