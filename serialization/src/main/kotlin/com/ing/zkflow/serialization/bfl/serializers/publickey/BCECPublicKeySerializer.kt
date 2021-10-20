package com.ing.zkflow.serialization.bfl.serializers.publickey

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zkflow.serialization.bfl.serializers.PublicKeySurrogate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey

object BCECPublicKeySerializer :
    SurrogateSerializer<BCECPublicKey, BCECSurrogate>(
        BCECSurrogate.serializer(),
        {
            val scheme = Crypto.findSignatureScheme(it)
            BCECSurrogate(scheme.schemeNumberID.toByte(), it.encoded)
        }
    )

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("a")
/**
 * Corresponds to Corda's Crypto.ECDSA_SECP256K1_SHA256 (88 bytes) and Crypto.ECDSA_SECP256R1_SHA256 (91 bytes).
 */
data class BCECSurrogate(
    val schemeID: Byte,
    @FixedLength([ENCODED_SIZE]) override val encoded: ByteArray
) : PublicKeySurrogate<BCECPublicKey> {
    override fun toOriginal() = toOriginal(schemeID.toInt()) as BCECPublicKey

    companion object {
        const val ENCODED_SIZE = 91
    }
}
