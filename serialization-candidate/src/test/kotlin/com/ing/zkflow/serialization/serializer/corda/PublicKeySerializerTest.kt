package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.PublicKey

class PublicKeySerializerTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `PublicKey must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(PublicKeys.EdDsa_0, PublicKeys.edDsaKey)
        engine.assertRoundTrip(PublicKeys.Rsa_0, PublicKeys.rsaKey)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `PublicKey's must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(PublicKeys.EdDsa_0, PublicKeys.edDsaKey).size shouldBe
            engine.serialize(PublicKeys.EdDsa_0, Crypto.generateKeyPair(Crypto.EDDSA_ED25519_SHA512).public).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with PublicKeys must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(PublicKeys.serializer(), PublicKeys.keys)
    }

    @Suppress("ClassName")
    @Serializable
    data class PublicKeys(
        @Serializable(with = EdDsa_0::class) val edDsa: PublicKey,
        @Serializable(with = Rsa_0::class) val rsa: PublicKey
    ) {
        object EdDsa_0 : PublicKeySerializer(Crypto.EDDSA_ED25519_SHA512.schemeNumberID)
        object Rsa_0 : PublicKeySerializer(Crypto.RSA_SHA256.schemeNumberID)

        companion object {
            val edDsaKey: PublicKey = Crypto.generateKeyPair(Crypto.EDDSA_ED25519_SHA512).public
            val rsaKey: PublicKey = Crypto.generateKeyPair(Crypto.RSA_SHA256).public
            val keys = PublicKeys(edDsaKey, rsaKey)
        }
    }
}
