package com.ing.zknotary.testing.dsl

import com.ing.zknotary.common.crypto.zincAlgorithm
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.common.zkp.ZincZKTransactionService
import net.corda.core.identity.Party
import net.corda.core.internal.HashAgility
import net.corda.core.node.ServiceHub
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.common.internal.addNotary
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.internal.MockNetworkParametersStorage

@JvmOverloads
public fun ServiceHub.zkLedger(
    notary: Party = TestIdentity.fresh("ledger notary").party,
    zkService: ZKTransactionService = ZincZKTransactionService(this),
    script: LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.() -> Unit
): LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter> {
    val currentParameters = networkParametersService.run {
        lookup(currentHash) ?: throw IllegalStateException("Current network parameters not found, $currentHash")
    }
    if (currentParameters.notaries.none { it.identity == notary }) {
        // Add the notary to the whitelist. Otherwise no constructed transactions will verify.
        val newParameters = currentParameters.addNotary(notary)
        (networkParametersService as MockNetworkParametersStorage).setCurrentParametersUnverified(newParameters)
    }

    HashAgility.init(zincAlgorithm)

    return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv {
        val interpreter = TestLedgerDSLInterpreter(this, zkService)
        LedgerDSL(interpreter, notary).apply {
            script()
        }
    }
}
