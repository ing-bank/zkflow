package com.ing.zknotary.common.zkp

import net.corda.core.crypto.Crypto
import org.junit.Test

class FixedKeyPairTest {
    @Test
    fun `fixed key pair must generate the same key pair`() {
        val kp1 = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
        val kp2 = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)

        assert(kp1.public.encoded.zip(kp2.public.encoded).all { it.first == it.second }) { "Public keys coincide" }
        assert(kp1.private.encoded.zip(kp2.private.encoded).all { it.first == it.second }) { "Public keys coincide" }
    }
}
