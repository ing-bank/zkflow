package com.ing.zkflow.annotated

import com.ing.zkflow.Converter
import com.ing.zkflow.annotated.pilot.infra.EdDSAPublicKeyConverter
import com.ing.zkflow.annotated.pilot.infra.EdDSAPublicKeySurrogate
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EcDSA_K1
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import net.corda.core.crypto.Crypto
import java.security.PublicKey

@Suppress("SpellCheckingInspection")
@ZKP
data class WrapsPublicKeys(
    val eddsa: @EdDSA PublicKey = pkEdDSA,
    val ecdsaK1: @EcDSA_K1 PublicKey = pkEcDSA_K1,
    val pkFullyCustom: @Converter<PublicKey, EdDSAPublicKeySurrogate>(EdDSAPublicKeyConverter::class) PublicKey = pkEdDSA,
) {
    companion object {
        private val pkEdDSA: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
        private val pkEcDSA_K1: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.ECDSA_SECP256K1_SHA256)
    }
}
