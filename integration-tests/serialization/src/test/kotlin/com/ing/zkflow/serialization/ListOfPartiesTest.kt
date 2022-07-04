package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.PublicKey

class ListOfPartiesTest : SerializerTest {
    // Setup
    @ZKP
    @Suppress("UnusedPrivateMember")
    data class ListOfParties(
        val anonymousParties: @Size(3) List<@EdDSA AnonymousParty> = listOf(someAnonymous),
    ) {
        companion object {
            private val pk: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
            internal val someAnonymous = AnonymousParty(pk)
        }
    }

    // Resolved
    @Suppress("ClassName", "UnusedPrivateMember")
    @Serializable
    data class ListOfPartiesResolved constructor (
        @Serializable(with = AnonymousParties_0::class) val anonymousParties: @Contextual List<@Contextual AnonymousParty> = listOf(
            ListOfParties.someAnonymous
        ),
    ) {
        object AnonymousParties_0 : FixedLengthListSerializer<AnonymousParty>(3, AnonymousParties_1)
        object AnonymousParties_1 : AnonymousPartySerializer(4)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `ListOfParties makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(ListOfPartiesTest_ListOfParties_Serializer, ListOfParties())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ListOfParties generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            ListOfPartiesResolved.serializer(),
            ListOfPartiesResolved()
        ) shouldBe
            engine.serialize(ListOfPartiesTest_ListOfParties_Serializer, ListOfParties())
    }
}
