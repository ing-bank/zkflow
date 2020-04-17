package com.ing.zknotary.common.zkp

interface Prover {
    fun prove(witness: ByteArray, instance: ByteArray): Proof
}