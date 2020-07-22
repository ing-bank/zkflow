package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.util.Result
import net.corda.core.serialization.SerializeAsToken

interface ZKService : SerializeAsToken {
    fun prove(witness: ByteArray): Result<String, String>
    fun verify(proof: String): Result<Unit, String>
}
