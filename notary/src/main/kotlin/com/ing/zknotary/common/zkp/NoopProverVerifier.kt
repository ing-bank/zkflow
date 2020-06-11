package com.ing.zknotary.common.zkp

import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
internal class NoopZKZKProverService(val serviceHub: AppServiceHub) : SingletonSerializeAsToken(), ZKProverService {
    override fun prove(witness: ByteArray, instance: ByteArray): Proof {
        return Proof(ByteArray(0))
    }
}

@CordaService
internal class NoopZKVerifierService(val serviceHub: AppServiceHub) : SingletonSerializeAsToken(), ZKVerifierService {
    override fun verify(proof: Proof, instance: ByteArray) {
        // No exception is success
    }
}
