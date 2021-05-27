package com.ing.zknotary.common.serialization.bfl.serializers.publickey

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.PublicKeySurrogate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.i2p.crypto.eddsa.EdDSAPublicKey

object EdDSAPublicKeySerializer :
    SurrogateSerializer<EdDSAPublicKey, EdDSASurrogate>(
        EdDSASurrogate.serializer(),
        { EdDSASurrogate(it.encoded) }
    )

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("d")
/**
 * Corresponds to Corda's Crypto.EDDSA_ED25519_SHA512 (44 bytes).
 */
data class EdDSASurrogate(@FixedLength([ENCODED_SIZE]) override val encoded: ByteArray) :
    PublicKeySurrogate<EdDSAPublicKey> {
    override fun toOriginal() = toOriginal(Crypto.EDDSA_ED25519_SHA512.schemeNumberID) as EdDSAPublicKey

    companion object {
        const val ENCODED_SIZE = 44
    }
}
