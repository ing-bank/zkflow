package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.util.Result
import net.corda.core.serialization.SerializeAsToken

interface ZKService : SerializeAsToken {
    fun prove(witness: ByteArray): Result<Proof, String>
    fun verify(proof: Proof): Result<Unit, String>
}
