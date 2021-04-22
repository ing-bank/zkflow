package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECPublicKeySerializer
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSAPublicKeySerializer
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256PublicKeySerializer
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSAPublicKeySerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import java.security.PublicKey

/**
 * Support for public keys variant defined in Corda.
 */
object CordaSignatureSchemeToSerializers {
    private val scheme2serializer = mapOf(
        Crypto.RSA_SHA256.schemeNumberID to
            SerializersModule { polymorphic(PublicKey::class) { subclass(BCRSAPublicKeySerializer) } },
        Crypto.ECDSA_SECP256K1_SHA256.schemeNumberID to
            SerializersModule { polymorphic(PublicKey::class) { subclass(BCECPublicKeySerializer) } },
        Crypto.ECDSA_SECP256R1_SHA256.schemeNumberID to
            SerializersModule { polymorphic(PublicKey::class) { subclass(BCECPublicKeySerializer) } },
        Crypto.EDDSA_ED25519_SHA512.schemeNumberID to
            SerializersModule { polymorphic(PublicKey::class) { subclass(EdDSAPublicKeySerializer) } },
        Crypto.SPHINCS256_SHA256.schemeNumberID to
            SerializersModule { polymorphic(PublicKey::class) { subclass(BCSphincs256PublicKeySerializer) } },
    )

    fun serializersModuleFor(scheme: SignatureScheme): SerializersModule =
        scheme2serializer.getOrElse(scheme.schemeNumberID) { error("Unsupported signature scheme") }
}

interface PublicKeySurrogate<T : PublicKey> : Surrogate<T> {
    val encoded: ByteArray

    fun toOriginal(schemeID: Int): PublicKey {
        val scheme = Crypto.findSignatureScheme(schemeID)
        return Crypto.decodePublicKey(scheme, encoded)
    }
}
