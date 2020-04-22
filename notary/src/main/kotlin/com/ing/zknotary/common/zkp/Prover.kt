package com.ing.zknotary.common.zkp

import net.corda.core.serialization.SerializeAsToken

interface Prover : SerializeAsToken {
    fun prove(witness: ByteArray, instance: ByteArray): Proof
}
