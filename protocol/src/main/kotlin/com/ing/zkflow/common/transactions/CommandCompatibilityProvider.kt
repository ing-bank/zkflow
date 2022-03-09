package com.ing.zkflow.common.transactions

import net.corda.core.contracts.CommandData
import java.util.ServiceLoader


fun CommandData.verify(zkltx: ZKLedgerTransaction) {
    val provider = CommandCompatibilityProviderServiceLoader.providers[this.javaClass] ?: throw CommandCompatibilityProviderNotFound(this.javaClass)
    provider.verify(zkltx)
}

class CommandCompatibilityProviderNotFound(clazz: Class<CommandData>): Exception("Compatibility provider not defined for command class $clazz")

interface CommandCompatibilityProvider {
    val commandDataClass: Class<CommandData>
    fun verify(zkltx: ZKLedgerTransaction)
}

/**
 * This class can be used to switch off non-ZKP contract validation for a given Command
 */
open class AcceptAllCommandCompatibilityProvider(override val commandDataClass: Class<CommandData>) : CommandCompatibilityProvider {
    override fun verify(zkltx: ZKLedgerTransaction) { /* See no evil */ }
}

object CommandCompatibilityProviderServiceLoader {
    val providers: Map<Class<CommandData>, CommandCompatibilityProvider> by lazy {
        val providers: MutableMap<Class<CommandData>, CommandCompatibilityProvider> = mutableMapOf()
        ServiceLoader.load(CommandCompatibilityProvider::class.java).forEach {
            require(!providers.containsKey(it.commandDataClass)) { "Duplicate command compatibility providers found for class: ${it.commandDataClass}" }
            providers[it.commandDataClass] = it
        }
        providers
    }
}
