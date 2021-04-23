package com.ing.zknotary.common.serialization.bfl.serializers.publickey

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.PublicKeySurrogate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.i2p.crypto.eddsa.EdDSAPublicKey

object EdDSAPublicKeySerializer : KSerializer<EdDSAPublicKey> by (
    SurrogateSerializer(EdDSASurrogate.serializer()) {
        EdDSASurrogate(it.encoded)
    }
    )

@Suppress("ArrayInDataClass")
@Serializable
/**
 * Corresponds to Corda's Crypto.EDDSA_ED25519_SHA512 (44 bytes).
 */
data class EdDSASurrogate(@FixedLength([44]) override val encoded: ByteArray) :
    PublicKeySurrogate<EdDSAPublicKey> {
    override fun toOriginal() = toOriginal(Crypto.EDDSA_ED25519_SHA512.schemeNumberID) as EdDSAPublicKey
}
