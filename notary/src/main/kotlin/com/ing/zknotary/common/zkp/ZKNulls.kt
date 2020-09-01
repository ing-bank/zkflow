package com.ing.zknotary.common.zkp

import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

object ZKNulls {
    fun fixedKeyPair(signatureScheme: SignatureScheme): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(signatureScheme.algorithmName, Crypto.findProvider(signatureScheme.providerName))
        var insecureRandom = SecureRandom.getInstance("SHA1PRNG")
        insecureRandom.setSeed(ByteArray(1) { 1 })

        if (signatureScheme.algSpec != null)
            keyPairGenerator.initialize(signatureScheme.algSpec, insecureRandom)
        else
            keyPairGenerator.initialize(signatureScheme.keySize!!, insecureRandom)
        return keyPairGenerator.generateKeyPair()
    }

    // TODO: Make a function accepting a signature scheme
    val NULL_PUBLIC_KEY: PublicKey = fixedKeyPair(Crypto.EDDSA_ED25519_SHA512).public
    val NULL_ANONYMOUS_PARTY = AnonymousParty(NULL_PUBLIC_KEY)
    val NULL_PARTY = Party(CordaX500Name.parse("O=BOGUS,L=New York,C=US"), NULL_PUBLIC_KEY)

    /** A signature with a key and value of zero. Useful when you want a signature object that you know won't ever be used. */
//    val NULL_SIGNATURE = TransactionSignature(ByteArray(32), NullPublicKey, SignatureMetadata(1, -1))
}
