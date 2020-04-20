package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.util.toNative
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class ZincZKVerifierNative(val serviceHub: ServiceHub) : SingletonSerializeAsToken(), ZKVerifier {
    override fun verify(proof: Proof, instance: ByteArray) {
        val verifierKeyPath: String = serviceHub.getAppContext().config.getString("proverKeyPath")

        val result = ZincVerifierLibrary.INSTANCE.verify(
            verifierKeyPath,
            proof.bytes.toNative(),
            proof.bytes.size,
            instance.toNative(),
            instance.size
        )
        if (result != 1) throw ZKProofVerificationException("ZK Proof verification failed: reason understandably not given. ;-)")
    }

    interface ZincVerifierLibrary : Library {
        fun verify(
            verifierKeyPath: String,
            proof: Pointer,
            proofSize: Int,
            instance: Pointer,
            instanceSize: Int
        ): Int

        companion object {
            val INSTANCE = Native.load("zinc_verifier", ZincVerifierLibrary::class.java) as ZincVerifierLibrary
        }
    }
}

