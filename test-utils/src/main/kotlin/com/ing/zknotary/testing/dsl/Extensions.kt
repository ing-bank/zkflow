package com.ing.zknotary.testing.dsl

import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.testing.common.internal.addNotary
import net.corda.testing.core.TestIdentity
import net.corda.testing.internal.withTestSerializationEnvIfNotSet
import net.corda.testing.node.internal.MockNetworkParametersStorage

@JvmOverloads
public fun ServiceHub.zkLedger(
    notary: Party = TestIdentity.fresh("ledger notary").party,
    script: ZKLedgerDSL<ZKTestTransactionDSLInterpreter, ZKTestLedgerDSLInterpreter>.() -> Unit
): ZKLedgerDSL<ZKTestTransactionDSLInterpreter, ZKTestLedgerDSLInterpreter> {
    val currentParameters = networkParametersService.run {
        lookup(currentHash) ?: throw IllegalStateException("Current network parameters not found, $currentHash")
    }
    if (currentParameters.notaries.none { it.identity == notary }) {
        // Add the notary to the whitelist. Otherwise no constructed transactions will verify.
        val newParameters = currentParameters.addNotary(notary)
        (networkParametersService as MockNetworkParametersStorage).setCurrentParametersUnverified(newParameters)
    }

    return withTestSerializationEnvIfNotSet {
        val interpreter = ZKTestLedgerDSLInterpreter(this)
        ZKLedgerDSL(interpreter, notary).apply {
            script()
        }
    }
}
