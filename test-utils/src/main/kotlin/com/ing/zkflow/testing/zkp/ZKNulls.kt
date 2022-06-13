package com.ing.zkflow.testing.zkp

import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

public object ZKNulls {
    private val keyPairs = mutableMapOf<SignatureScheme, KeyPair>()

    public fun fixedKeyPair(signatureScheme: SignatureScheme): KeyPair {
        return keyPairs.getOrPut(signatureScheme) {
            val keyPairGenerator = KeyPairGenerator.getInstance(signatureScheme.algorithmName, Crypto.findProvider(signatureScheme.providerName))
            val insecureRandom = SecureRandom.getInstance("SHA1PRNG")
            insecureRandom.setSeed(ByteArray(1) { 1 })

            if (signatureScheme.algSpec != null)
                keyPairGenerator.initialize(signatureScheme.algSpec, insecureRandom)
            else
                keyPairGenerator.initialize(signatureScheme.keySize!!, insecureRandom)
            keyPairGenerator.generateKeyPair()
        }
    }

    public fun fixedAnonymousParty(signatureScheme: SignatureScheme): AnonymousParty =
        AnonymousParty(fixedKeyPair(signatureScheme).public)

    // TODO: Make a function accepting a signature scheme
    public val NULL_PUBLIC_KEY: PublicKey = fixedKeyPair(Crypto.EDDSA_ED25519_SHA512).public
    public val NULL_ANONYMOUS_PARTY: AnonymousParty = AnonymousParty(NULL_PUBLIC_KEY)
    public val NULL_PARTY: Party = Party(CordaX500Name.parse("O=BOGUS,L=New York,C=US"), NULL_PUBLIC_KEY)

    /** A signature with a key and value of zero. Useful when you want a signature object that you know won't ever be used. */
}
