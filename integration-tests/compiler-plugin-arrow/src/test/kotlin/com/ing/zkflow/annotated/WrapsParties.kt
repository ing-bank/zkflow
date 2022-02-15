package com.ing.zkflow.annotated

import com.ing.zkflow.AnonymousParty_EdDSA
import com.ing.zkflow.AnonymousParty_EdDSA_Converter
import com.ing.zkflow.Converter
import com.ing.zkflow.CordaX500NameConverter
import com.ing.zkflow.CordaX500NameSurrogate
import com.ing.zkflow.Party_EdDSA
import com.ing.zkflow.Party_EdDSA_Converter
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.CordaX500NameSpec
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.PublicKey

@ZKP
data class WrapsParties(
    val anonymousParty: @EdDSA AnonymousParty = someAnonymous,
    val anonymousPartyFullyCustom: @Converter<AnonymousParty, AnonymousParty_EdDSA>(
        AnonymousParty_EdDSA_Converter::class
    ) AnonymousParty = someAnonymous,

    val party: @EdDSA Party = someParty,
    val partyCX500Custom: @EdDSA @CordaX500NameSpec<CordaX500NameSurrogate>(CordaX500NameConverter::class) Party = someParty,
    val partyFullyCustom: @Converter<Party, Party_EdDSA>(Party_EdDSA_Converter::class) Party = someParty,
) {
    companion object {
        private val pk: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
        val someAnonymous = AnonymousParty(pk)
        val someParty = Party(CordaX500Name(organisation = "IN", locality = "AMS", country = "NL"), pk)
    }
}
