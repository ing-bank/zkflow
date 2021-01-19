package com.ing.zknotary.ksp.generator

import com.ing.zknotary.annotations.DefaultValue
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

class PublicKeyDefaultValue : DefaultValue {
    override val default: PublicKey
        get() {
            val keygen = KeyPairGenerator.getInstance("DSA", "SUN")
            val insecureRandom = SecureRandom.getInstance("SHA1PRNG")
            insecureRandom.setSeed(ByteArray(1) { 1 })
            keygen.initialize(1024, insecureRandom)
            val keypair = keygen.genKeyPair()
            return keypair.public
        }
}
