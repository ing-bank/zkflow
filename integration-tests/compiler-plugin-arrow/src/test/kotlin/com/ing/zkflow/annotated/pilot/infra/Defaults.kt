@file:Suppress("MatchingDeclarationName")

package com.ing.zkflow.annotated.pilot.infra

import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.testing.zkp.ZKNulls.fixedKeyPair
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name

object EdDSAAnonymousPartyDefaultProvider : DefaultProvider<AnonymousPartySurrogate_EdDSA> {
    override val default: AnonymousPartySurrogate_EdDSA
        get() {
            val pk = fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
            return AnonymousPartySurrogate_EdDSA(pk.public.encoded)
        }
}

val fixedCordaX500Name = CordaX500Name.parse("O=BOGUS,L=New York,C=US")
