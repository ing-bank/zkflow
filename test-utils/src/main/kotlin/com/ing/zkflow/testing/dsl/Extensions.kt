package com.ing.zkflow.testing.dsl

import TestTransactionDSLInterpreter
import TestZKLedgerDSLInterpreter
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.serialization.SerializersModuleRegistry
import com.ing.zkflow.serialization.bfl.TestCordaSerializers
import com.ing.zkflow.testing.zkp.MockZKNetworkParameters
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
    zkService: TestDSLZKTransactionService = TestDSLZincZKTransactionService(this),
    zkNetworkParameters: ZKNetworkParameters = MockZKNetworkParameters,
    script: LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter, TestZKLedgerDSLInterpreter>.() -> Unit
): LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter, TestZKLedgerDSLInterpreter> {
    val currentParameters = networkParametersService.run {
        lookup(currentHash) ?: throw IllegalStateException("Current network parameters not found, $currentHash")
    }
    if (currentParameters.notaries.none { it.identity == notary }) {
        // Add the notary to the whitelist. Otherwise no constructed transactions will verify.
        val newParameters = currentParameters.addNotary(notary)
        (networkParametersService as MockNetworkParametersStorage).setCurrentParametersUnverified(newParameters)
    }

    HashAgility.init(null, zkNetworkParameters.digestAlgorithm::class.java.name)

    // TODO: this should not be registered here, since it should not be aware of BFL, but until we have a better loading mechanism this is the least bad place
    SerializersModuleRegistry.register(TestCordaSerializers.module)

    return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv {
        val interpreter = TestZKLedgerDSLInterpreter(this, zkService, zkNetworkParameters)
        LedgerDSL(interpreter, notary, zkNetworkParameters).apply {
            script()
        }
    }
}
