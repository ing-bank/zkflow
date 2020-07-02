package com.ing.zknotary.common.states

import net.corda.core.KeepForDJVM
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable

@KeepForDJVM
@CordaSerializable
data class ZKStateRef(
    val id: SecureHash
) {
    override fun toString() = id.toString()

    companion object {
        fun empty() = ZKStateRef(SecureHash.zeroHash)
    }
}
