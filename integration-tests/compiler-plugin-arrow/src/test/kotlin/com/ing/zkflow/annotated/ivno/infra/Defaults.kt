@file:Suppress("MatchingDeclarationName")

package com.ing.zkflow.annotated.ivno.infra

import com.ing.zkflow.DefaultProvider
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import net.corda.core.identity.CordaX500Name
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom

object EdDSAAnonymousPartyDefaultProvider : DefaultProvider<EdDSAAnonymousParty> {
    override val default: EdDSAAnonymousParty
        get() {
            val pk = fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
            return EdDSAAnonymousParty(pk.public.encoded)
        }
}

fun fixedKeyPair(signatureScheme: SignatureScheme): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance(signatureScheme.algorithmName, Crypto.findProvider(signatureScheme.providerName))
    val insecureRandom = SecureRandom.getInstance("SHA1PRNG")
    insecureRandom.setSeed(ByteArray(1) { 1 })

    if (signatureScheme.algSpec != null)
        keyPairGenerator.initialize(signatureScheme.algSpec, insecureRandom)
    else
        keyPairGenerator.initialize(signatureScheme.keySize!!, insecureRandom)

    return keyPairGenerator.generateKeyPair()
}

val fixedCordaX500Name = CordaX500Name.parse("O=BOGUS,L=New York,C=US")
