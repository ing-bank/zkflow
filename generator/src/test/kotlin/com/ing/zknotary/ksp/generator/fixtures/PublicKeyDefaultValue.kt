package com.ing.zknotary.ksp.generator.fixtures

import com.ing.zknotary.annotations.DefaultValue
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

class PublicKeyDefaultValue : DefaultValue {
    override val default: PublicKey
        get() {
            val insecureRandom = SecureRandom.getInstance("SHA1PRNG")
            insecureRandom.setSeed(ByteArray(1) { 1 })
            return generate(insecureRandom)
        }

    val random: PublicKey
        get() = generate(SecureRandom.getInstance("SHA1PRNG"))

    private fun generate(random: SecureRandom): PublicKey {
        val keygen = KeyPairGenerator.getInstance("DSA", "SUN")
        keygen.initialize(1024, random)
        val keypair = keygen.genKeyPair()
        return keypair.public
    }
}
