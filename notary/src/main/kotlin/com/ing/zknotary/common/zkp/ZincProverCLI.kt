package com.ing.zknotary.common.zkp

import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class ZincProverCLI(val serviceHub: ServiceHub) : SingletonSerializeAsToken(), Prover {
    override fun prove(witness: ByteArray, instance: ByteArray): Proof {
        val proverKeyPath: String = serviceHub.getAppContext().config.getString("proverKeyPath")

        // write witness to file
        // write instance to file
        // call zargo prove with arguments for witness, instance and prover key location and save result as proof ByteArray
        return Proof(ByteArray(0))
    }
}

