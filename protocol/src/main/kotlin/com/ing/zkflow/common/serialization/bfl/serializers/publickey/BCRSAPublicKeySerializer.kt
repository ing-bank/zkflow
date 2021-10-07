package com.ing.zkflow.common.serialization.bfl.serializers.publickey

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zkflow.common.serialization.bfl.serializers.PublicKeySurrogate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey

object BCRSAPublicKeySerializer :
    SurrogateSerializer<BCRSAPublicKey, BCRSASurrogate>(
        BCRSASurrogate.serializer(),
        { BCRSASurrogate(it.encoded) }
    )

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("b")
/**
 * Corresponds to Corda's Crypto.RSA_SHA256 (422 bytes).
 */
data class BCRSASurrogate(@FixedLength([ENCODED_SIZE]) override val encoded: ByteArray) : PublicKeySurrogate<BCRSAPublicKey> {
    override fun toOriginal() = toOriginal(Crypto.RSA_SHA256.schemeNumberID) as BCRSAPublicKey

    companion object {
        const val ENCODED_SIZE = 422
    }
}
