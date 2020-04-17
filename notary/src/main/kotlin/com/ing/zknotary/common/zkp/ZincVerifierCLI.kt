package com.ing.zknotary.common.zkp

class ZincVerifierCLI(private val verifierKeyPath: String) : Verifier {
    override fun verify(proof: Proof, instance: ByteArray) {
        // write proof to file
        // write instance to file
        // call zargo verify with arguments for proof, instance and verifier key location and save result
        // if (result != 1) throw ZKProofVerificationException("ZK Proof verification failed: reason understandably not given. ;-)")
    }
}

