package com.ing.zkflow.resolved

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.PublicKey

@Suppress("ClassName", "UnusedPrivateMember")
@Serializable
data class WrapsListOfParties constructor (
    @Serializable(with = AnonymousParties_0::class) val anonymousParties: @Contextual @Size(3) List<@Contextual @EdDSA AnonymousParty> = listOf(someAnonymous),
) {
    companion object {
        private val pk: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
        private val someAnonymous = AnonymousParty(pk)
    }

    @Transient
    private val mustBeTransient = anonymousParties + someAnonymous

    object AnonymousParties_0 : FixedLengthListSerializer<AnonymousParty>(3, AnonymousParties_1)
    object AnonymousParties_1 : AnonymousPartySerializer(4)
}

class WrapsListOfPartiesTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsListOfParties make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsListOfParties.serializer(),
            com.ing.zkflow.annotated.WrapsListOfParties()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsListOfParties generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsListOfParties.serializer(),
            com.ing.zkflow.annotated.WrapsListOfParties()
        ) shouldBe
            engine.serialize(WrapsListOfParties.serializer(), WrapsListOfParties())
    }
}
