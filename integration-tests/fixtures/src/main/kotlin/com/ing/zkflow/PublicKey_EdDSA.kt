@file:Suppress("ClassName")

package com.ing.zkflow

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

@ZKP
@Suppress("ArrayInDataClass")
data class PublicKey_EdDSA(
    val key: @Size(ED_DSA_KEY_LENGTH) ByteArray
) : Surrogate<PublicKey> {
    override fun toOriginal(): PublicKey =
        KeyFactory
            .getInstance("EdDSA")
            .generatePublic(X509EncodedKeySpec(algorithmIdentifier + key))

    companion object {
        /**
         * This is a hack to directly specify algorithm identifier to make the encoding agree with X509 specification.
         * Specs can be found in [X509EncodedKeySpec] (/java/security/spec/X509EncodedKeySpec.java).
         * This way direct construction is avoided, because it does not seem straightforward.
         * Encoding construction can be found in net.i2p.crypto.eddsa.EdDSAPublicKey.getEncoded.
         */
        val algorithmIdentifier = byteArrayOf(48, 42, 48, 5, 6, 3, 43, 101, 112, 3, 33, 0)

        const val ED_DSA_KEY_LENGTH = 32

        const val ED_DSA_X509_ENCODING_LENGTH = 44
    }
}

object PublicKey_EdDSA_Converter : ConversionProvider<PublicKey, PublicKey_EdDSA> {
    override fun from(original: PublicKey): PublicKey_EdDSA {
        require(original.encoded.size == PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH) {
            "`${original.algorithm}` key encoding must be ${PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH} bytes long; got ${original.encoded.size}"
        }

        // Ignore first 12 bytes.
        // For more information see Specs can be found in [X509EncodedKeySpec] (/java/security/spec/X509EncodedKeySpec.java).
        // The tail 32 bytes are the actual key.
        val key = original.encoded.copyOfRange(
            PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH - PublicKey_EdDSA.ED_DSA_KEY_LENGTH,
            PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH
        )
        require(key.size == PublicKey_EdDSA.ED_DSA_KEY_LENGTH) {
            "`${original.algorithm}` key must be ${PublicKey_EdDSA.ED_DSA_KEY_LENGTH} bytes long, got ${key.size} bytes"
        }

        return PublicKey_EdDSA(key)
    }
}
