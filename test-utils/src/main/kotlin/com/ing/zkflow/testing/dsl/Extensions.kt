package com.ing.zkflow.testing.dsl

import TestTransactionDSLInterpreter
import TestZKLedgerDSLInterpreter
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.serialization.BFLSerializationSchemeCandidate.Companion.ZkCommandDataSerializerRegistry
import com.ing.zkflow.serialization.SerializersModuleRegistry
import com.ing.zkflow.serialization.bfl.TestCordaSerializers
import com.ing.zkflow.serialization.serializer.corda.DummyCommandDataSerializer
import com.ing.zkflow.testing.zkp.MockZKNetworkParameters
import net.corda.core.identity.Party
import net.corda.core.internal.HashAgility
import net.corda.core.node.ServiceHub
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.common.internal.addNotary
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.internal.MockNetworkParametersStorage

@JvmOverloads
public fun ServiceHub.zkLedger(
    notary: Party = TestIdentity.fresh("ledger notary").party,
    zkService: TestDSLZKTransactionService = TestDSLZincZKTransactionService(this),
    zkNetworkParameters: ZKNetworkParameters = MockZKNetworkParameters(),
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

    // Register some test serializers that are required for DSL-inserted transaction components
    SerializersModuleRegistry.register(TestCordaSerializers.module)
    ZkCommandDataSerializerRegistry.register(DummyCommandData::class, DummyCommandDataSerializer)

    return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv {
        val interpreter = TestZKLedgerDSLInterpreter(this, zkService, zkNetworkParameters)
        LedgerDSL(interpreter, notary, zkNetworkParameters).apply {
            script()
        }
    }
}
