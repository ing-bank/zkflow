package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.util.toNative
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference

class ZincProverNative(private val proverKeyPath: String) : Prover {
    override fun prove(witness: ByteArray, instance: ByteArray): Proof {
        val proofRef = PointerByReference()
        val proofSizeRef = IntByReference()
        ZincProverLibrary.INSTANCE.prove(proverKeyPath, proofRef, proofSizeRef, witness.toNative(), witness.size, instance.toNative(), instance.size)

        val proofSize = proofSizeRef.value
        val proofBytes = proofRef.value.getByteArray(0, proofSize)

        return Proof(proofBytes)
        // return Proof(ByteArray(0))
    }

    private interface ZincProverLibrary : Library {
        fun prove(
            proverKeyPath: String,
            proofRef: PointerByReference,
            proofSizeRef: IntByReference,
            witness: Pointer,
            witnessSize: Int,
            instance: Pointer,
            instanceSize: Int
        ): Int

        companion object {
            val INSTANCE = Native.load("zinc_prover", ZincProverLibrary::class.java) as ZincProverLibrary
        }
    }
}

