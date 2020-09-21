package com.ing.zknotary.common.states

import com.ing.zknotary.common.dactyloscopy.Fingerprintable
import net.corda.core.KeepForDJVM
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable

@KeepForDJVM
@CordaSerializable
data class ZKStateRef(
    val id: SecureHash
) : Fingerprintable {
    override fun toString() = id.toString()

    override fun fingerprint(): ByteArray = id.bytes

    companion object {
        fun empty() = ZKStateRef(SecureHash.zeroHash)
    }
}
