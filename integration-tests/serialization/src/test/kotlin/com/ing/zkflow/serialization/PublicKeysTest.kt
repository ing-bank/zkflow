package com.ing.zkflow.serialization

import com.ing.zkflow.PublicKey_EdDSA
import com.ing.zkflow.PublicKey_EdDSASerializer
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EcDSA_K1
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.PublicKey

class PublicKeysTest : SerializerTest {
    // Setup
    @Suppress("SpellCheckingInspection")
    @ZKP
    data class PublicKeys(
        val eddsa: @EdDSA PublicKey = pkEdDSA,
        val ecdsaK1: @EcDSA_K1 PublicKey = pkEcDSA_K1,
        val pkFullyCustom: @Via<PublicKey_EdDSA> PublicKey = pkEdDSA,
    ) {
        companion object {
            private val pkEdDSA: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
            private val pkEcDSA_K1: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.ECDSA_SECP256K1_SHA256)
        }
    }

    // Resolved
    @Suppress("SpellCheckingInspection", "ClassName")
    @Serializable
    data class PublicKeysResolved(
        @Serializable(with = Eddsa_0::class) val eddsa: @EdDSA @Contextual PublicKey = pkEdDSA,
        @Serializable(with = EcdsaK1_0::class) val ecdsaK1: @EcDSA_K1 @Contextual PublicKey = pkEcDSA_K1,
        @Serializable(with = PkFullyCustom_0::class) val pkFullyCustom: @Via<PublicKey_EdDSA> @Contextual PublicKey = pkEdDSA
    ) {
        companion object {
            private val pkEdDSA: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
            private val pkEcDSA_K1: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.ECDSA_SECP256K1_SHA256)
        }

        object Eddsa_0 : PublicKeySerializer(4)
        object EcdsaK1_0 : PublicKeySerializer(2)
        object PkFullyCustom_0 : WrappedFixedLengthKSerializer<PublicKey>(
            PublicKey_EdDSASerializer,
            PublicKey_EdDSA::class.java.isEnum
        )
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `PublicKeys makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(PublicKeysTestPublicKeysSerializer, PublicKeys())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `PublicKeys generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            PublicKeysResolved.serializer(),
            PublicKeysResolved()
        ) shouldBe
            engine.serialize(PublicKeysTestPublicKeysSerializer, PublicKeys())
    }
}
