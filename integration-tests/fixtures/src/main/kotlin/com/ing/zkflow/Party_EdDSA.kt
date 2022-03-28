@file:Suppress("ClassName")

package com.ing.zkflow

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKPSurrogate
import net.corda.core.crypto.Crypto
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party

@ZKPSurrogate(Party_EdDSA_Converter::class)
@Suppress("ArrayInDataClass")
data class Party_EdDSA(
    val cordaX500Name: @Size(CordaX500NameSurrogate.UPPER_BOUND) String,
    val encodedEdDSA: @Size(PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH) ByteArray
) : Surrogate<Party> {
    override fun toOriginal() = Party(
        CordaX500Name.parse(cordaX500Name),
        Crypto.decodePublicKey(Crypto.EDDSA_ED25519_SHA512, encodedEdDSA)
    )
}

object Party_EdDSA_Converter : ConversionProvider<Party, Party_EdDSA> {
    override fun from(original: Party): Party_EdDSA {
        require(original.owningKey.algorithm == "EdDSA") {
            "This converter only accepts parties with EdDSA keys"
        }

        return Party_EdDSA(
            original.name.toString(),
            original.owningKey.encoded
        )
    }
}
