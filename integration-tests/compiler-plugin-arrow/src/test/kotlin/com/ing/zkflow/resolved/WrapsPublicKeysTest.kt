package com.ing.zkflow.resolved

import com.ing.zkflow.Converter
import com.ing.zkflow.PublicKey_EdDSA
import com.ing.zkflow.PublicKey_EdDSA_Converter
import com.ing.zkflow.annotations.corda.EcDSA_K1
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import net.corda.core.crypto.Crypto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.PublicKey

@Suppress("SpellCheckingInspection", "ClassName")
@Serializable
data class WrapsPublicKeys(
    @Serializable(with = Eddsa_0::class) val eddsa: @EdDSA @Contextual PublicKey = pkEdDSA,
    @Serializable(with = EcdsaK1_0::class) val ecdsaK1: @EcDSA_K1 @Contextual PublicKey = pkEcDSA_K1,
    @Serializable(with = PkFullyCustom_0::class) val pkFullyCustom: @Converter<PublicKey, PublicKey_EdDSA>(
        PublicKey_EdDSA_Converter::class
    ) @Contextual PublicKey = pkEdDSA
) {
    companion object {
        private val pkEdDSA: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
        private val pkEcDSA_K1: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.ECDSA_SECP256K1_SHA256)
    }

    object Eddsa_0 : PublicKeySerializer(4)
    object EcdsaK1_0 : PublicKeySerializer(2)
    object PkFullyCustom_0 : SurrogateSerializer<PublicKey, PublicKey_EdDSA>(
        PublicKey_EdDSA.serializer(), { PublicKey_EdDSA_Converter.from(it) }
    )
}

class WrapsPublicKeysTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsPublicKeys make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsPublicKeys.serializer(),
            com.ing.zkflow.annotated.WrapsPublicKeys()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsPublicKeys generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsPublicKeys.serializer(),
            com.ing.zkflow.annotated.WrapsPublicKeys()
        ) shouldBe
            engine.serialize(WrapsPublicKeys.serializer(), WrapsPublicKeys())
    }
}
