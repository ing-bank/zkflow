package com.ing.zknotary.common.zkp

internal class NoopProver : Prover {
    override fun prove(witness: ByteArray, instance: ByteArray): Proof {
        return Proof(ByteArray(0))
    }
}

internal class NoopVerifier : Verifier {
    override fun verify(proof: Proof, instance: ByteArray) {
        // No exception is success
    }
}

