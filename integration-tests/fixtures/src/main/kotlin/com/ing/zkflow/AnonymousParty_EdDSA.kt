@file:Suppress("ClassName")

package com.ing.zkflow

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.testing.zkp.ZKNulls
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty

@ZKPSurrogate(AnonymousParty_EdDSA_Converter::class)
@Suppress("ArrayInDataClass")
data class AnonymousParty_EdDSA(
    val encodedEdDSA: @Size(PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH) ByteArray
) : Surrogate<AnonymousParty> {
    override fun toOriginal() = AnonymousParty(
        Crypto.decodePublicKey(Crypto.EDDSA_ED25519_SHA512, encodedEdDSA)
    )
}

object AnonymousParty_EdDSA_DefaultProvider : DefaultProvider<AnonymousParty> {
    override val default: AnonymousParty
        get() {
            return ZKNulls.fixedAnonymousParty(Crypto.EDDSA_ED25519_SHA512)
        }
}

object AnonymousParty_EdDSA_Converter : ConversionProvider<AnonymousParty, AnonymousParty_EdDSA> {
    override fun from(original: AnonymousParty): AnonymousParty_EdDSA {
        require(original.owningKey.algorithm == "EdDSA") {
            "This converter only accepts parties with EdDSA keys"
        }

        return AnonymousParty_EdDSA(original.owningKey.encoded)
    }
}
