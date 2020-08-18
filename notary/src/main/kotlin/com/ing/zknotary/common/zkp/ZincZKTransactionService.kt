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

    fun setup() = zkService.setup()
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
        val publicInputJson = publicInput.serialize(ZincSerializationFactory)
        return zkService.verify(proof, publicInputJson.bytes)
    }
}
