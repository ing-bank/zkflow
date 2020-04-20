package com.ing.zknotary.common.zkp

import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class ZincZKVerifierCLI(val serviceHub: ServiceHub) : SingletonSerializeAsToken(), ZKVerifier {
    override fun verify(proof: Proof, instance: ByteArray) {
        val verifierKeyPath: String = serviceHub.getAppContext().config.getString("proverKeyPath")

        // write proof to file
        // write instance to file
        // call zargo verify with arguments for proof, instance and verifier key location and save result
        // if (result != 1) throw ZKProofVerificationException("ZK Proof verification failed: reason understandably not given. ;-)")
    }
}

