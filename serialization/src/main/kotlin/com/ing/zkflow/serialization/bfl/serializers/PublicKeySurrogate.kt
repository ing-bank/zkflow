package com.ing.zkflow.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import net.corda.core.crypto.Crypto
import java.security.PublicKey

/**
 * Support for public keys variant defined in Corda.
 */
interface PublicKeySurrogate<T : PublicKey> : Surrogate<T> {
    val encoded: ByteArray

    fun toOriginal(schemeID: Int): PublicKey {
        val scheme = Crypto.findSignatureScheme(schemeID)
        return Crypto.decodePublicKey(scheme, encoded)
    }
}
