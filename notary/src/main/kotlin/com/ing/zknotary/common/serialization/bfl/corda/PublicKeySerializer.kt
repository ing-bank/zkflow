package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.api.Surrogate
import com.ing.zknotary.common.serialization.bfl.corda.publickey.BCECPublicKeySerializer
import com.ing.zknotary.common.serialization.bfl.corda.publickey.BCRSAPublicKeySerializer
import com.ing.zknotary.common.serialization.bfl.corda.publickey.BCSphincs256PublicKeySerializer
import com.ing.zknotary.common.serialization.bfl.corda.publickey.EdDSAPublicKeySerializer
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
    private val scheme2serializer = Crypto.supportedSignatureSchemes()
        .filter { it.schemeNumberID != Crypto.COMPOSITE_KEY.schemeNumberID }
        .map {
            val module = SerializersModule {
                polymorphic(PublicKey::class) {
                    when (it.schemeNumberID) {
                        Crypto.RSA_SHA256.schemeNumberID -> subclass(BCRSAPublicKeySerializer)
                        Crypto.ECDSA_SECP256K1_SHA256.schemeNumberID,
                        Crypto.ECDSA_SECP256R1_SHA256.schemeNumberID -> subclass(BCECPublicKeySerializer)
                        Crypto.EDDSA_ED25519_SHA512.schemeNumberID -> subclass(EdDSAPublicKeySerializer)
                        Crypto.SPHINCS256_SHA256.schemeNumberID -> subclass(BCSphincs256PublicKeySerializer)
                        else -> error("Unsupported signature scheme")
                    }
                }
            }
            Pair(it.schemeNumberID, module)
        }.toMap()

    infix fun serializerFor(scheme: SignatureScheme): SerializersModule =
        scheme2serializer.getOrElse(scheme.schemeNumberID) { error("Unsupported signature scheme") }
}

interface PublicKeySurrogate<T : PublicKey> : Surrogate<T> {
    val encoded: ByteArray

    fun toOriginal(schemeID: Int): PublicKey {
        val scheme = Crypto.findSignatureScheme(schemeID)
        return Crypto.decodePublicKey(scheme, encoded)
    }
}
