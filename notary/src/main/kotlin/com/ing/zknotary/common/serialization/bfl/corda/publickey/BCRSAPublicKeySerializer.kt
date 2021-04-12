package com.ing.zknotary.common.serialization.bfl.corda.publickey

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zknotary.common.serialization.bfl.corda.PublicKeySurrogate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey

object BCRSAPublicKeySerializer : KSerializer<BCRSAPublicKey> by (
        SurrogateSerializer(BCRSASurrogate.serializer()) {
            BCRSASurrogate(it.encoded)
        }
        )

@Suppress("ArrayInDataClass")
@Serializable
/**
 * Corresponds to Corda's Crypto.RSA_SHA256 (422 bytes).
 */
data class BCRSASurrogate(@FixedLength([422]) override val encoded: ByteArray) : PublicKeySurrogate<BCRSAPublicKey> {
    override fun toOriginal() = toOriginal(Crypto.RSA_SHA256.schemeNumberID) as BCRSAPublicKey
}
