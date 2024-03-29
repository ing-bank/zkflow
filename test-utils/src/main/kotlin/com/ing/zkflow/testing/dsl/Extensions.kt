package com.ing.zkflow.testing.dsl

import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zkflow.common.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistry
import com.ing.zkflow.common.zkp.ZKTransactionService
import com.ing.zkflow.serialization.DummyCommandDataSerializer
import com.ing.zkflow.serialization.register
import com.ing.zkflow.testing.dsl.services.TestDSLZincZKTransactionService
import com.ing.zkflow.testing.zkp.MockZKNetworkParameters
import com.ing.zkflow.util.tryNonFailing
import net.corda.core.identity.Party
import net.corda.core.internal.HashAgility
import net.corda.core.node.ServiceHub
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.common.internal.addNotary
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.internal.MockNetworkParametersStorage

/**
 * Set up a ledger context. Please note that if you change one of the zkService or zkVerifierTransactionStorage parameters, you
 * should also change the other: zkService should always use ZKVerifierTransactionStorage from the zkVerifierTransactionStorage parameter.
 * If it uses another one, there will be transaction resolution errors.
 */
@JvmOverloads
public fun ServiceHub.zkLedger(
    notary: Party = TestIdentity.fresh("ledger notary").party,
    zkNetworkParameters: ZKNetworkParameters = MockZKNetworkParameters(),
    zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage = InMemoryZKVerifierTransactionStorage(),
    zkService: ZKTransactionService = TestDSLZincZKTransactionService(this, zkVerifierTransactionStorage, zkNetworkParameters),
    script: ZKLedgerDSL<TestZKTransactionDSLInterpreter, TestZKLedgerDSLInterpreter>.() -> Unit
): ZKLedgerDSL<TestZKTransactionDSLInterpreter, TestZKLedgerDSLInterpreter> {
    val currentParameters = networkParametersService.run {
        lookup(currentHash) ?: throw IllegalStateException("Current network parameters not found, $currentHash")
    }
    if (currentParameters.notaries.none { it.identity == notary }) {
        // Add the notary to the whitelist. Otherwise, no constructed transactions will verify.
        val newParameters = currentParameters.addNotary(notary)
        (networkParametersService as MockNetworkParametersStorage).setCurrentParametersUnverified(newParameters)
    }

    HashAgility.init(null, zkNetworkParameters.digestAlgorithm::class.java.name)

    // Register some test serializers that are required for DSL-inserted transaction components
    tryNonFailing {
        CommandDataSerializerRegistry.register(DummyCommandData::class, DummyCommandDataSerializer)
    }

    return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv {
        val interpreter = TestZKLedgerDSLInterpreter(this, zkService, zkNetworkParameters, zkVerifierTransactionStorage)
        ZKLedgerDSL(interpreter, notary, zkNetworkParameters).apply { script() }
    }
}
