package com.ing.zknotary.common.zkp

import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
internal class NoopZKProver(val serviceHub: ServiceHub) : SingletonSerializeAsToken(), Prover {
    override fun prove(witness: ByteArray, instance: ByteArray): Proof {
        return Proof(ByteArray(0))
    }
}

@CordaService
internal class NoopZKVerifier(val serviceHub: ServiceHub) : SingletonSerializeAsToken(), ZKVerifier {
    override fun verify(proof: Proof, instance: ByteArray) {
        // No exception is success
    }
}

