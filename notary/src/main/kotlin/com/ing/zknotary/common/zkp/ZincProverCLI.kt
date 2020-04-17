package com.ing.zknotary.common.zkp

class ZincProverCLI(private val proverKeyPath: String) : Prover {
    override fun prove(witness: ByteArray, instance: ByteArray): Proof {
        // write witness to file
        // write instance to file
        // call zargo prove with arguments for witness, instance and prover key location and save result as proof ByteArray
        return Proof(ByteArray(0))
    }
}

