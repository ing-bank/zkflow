package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serializer.ZincSerializationFactory
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.serialization.serialize
import java.time.Duration

class ZincZKTransactionService(
    circuitFolder: String,
    artifactFolder: String,
    buildTimeout: Duration,
    setupTimeout: Duration,
    provingTimeout: Duration,
    verificationTimeout: Duration
) : ZKTransactionService, SingletonSerializeAsToken() {
    private val zkService =
        ZincZKService(circuitFolder, artifactFolder, buildTimeout, setupTimeout, provingTimeout, verificationTimeout)

    fun setup(force: Boolean = false) {
        if (force) {
            cleanup()
        }

        val circuit = Pair("${zkService.circuitFolder}/src", zkService.artifactFolder)
        CircuitManager.register(circuit)

        while (CircuitManager[circuit] == CircuitManager.Status.InProgress) {
            // An upper waiting time bound can be set up,
            // but this bound may be overly pessimistic.
            Thread.sleep(10 * 1000)
        }

        if (CircuitManager[circuit] == CircuitManager.Status.NotReady) {
            cleanup()
            CircuitManager.inProgress(circuit)
            zkService.setup()
            CircuitManager.cache(circuit)
        }
    }

    fun cleanup() = zkService.cleanup()

    override fun prove(witness: Witness): ByteArray {
        // It is ok to hardcode the ZincSerializationFactory here, as it is the ONLY way
        // this should be serialized in here. Makes no sense to make it injectable.
        val witnessJson = witness.serialize(ZincSerializationFactory).bytes
        return zkService.prove(witnessJson)
    }

    override fun verify(proof: ByteArray, publicInput: PublicInput) {
        // It is ok to hardcode the ZincSerializationFactory here, as it is the ONLY way
        // this should be serialized in here. Makes no sense to make it injectable.
        val publicInputJson = publicInput.serialize(ZincSerializationFactory).bytes
        return zkService.verify(proof, publicInputJson)
    }
}
