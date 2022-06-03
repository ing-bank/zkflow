package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import java.security.PublicKey

@ZKP
@Suppress("UnusedPrivateMember")
data class WrapsListOfParties(
    val anonymousParties: @Size(3) List<@EdDSA AnonymousParty> = listOf(someAnonymous),
) {
    companion object {
        private val pk: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
        private val someAnonymous = AnonymousParty(pk)
    }

    private val mustBeTransient = anonymousParties + someAnonymous
}
