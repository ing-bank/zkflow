package com.ing.zkflow.serialization

import com.ing.zkflow.AnonymousParty_EdDSA
import com.ing.zkflow.AnonymousParty_EdDSA_Serializer
import com.ing.zkflow.CordaX500NameConverter
import com.ing.zkflow.CordaX500NameSurrogate
import com.ing.zkflow.CordaX500NameSurrogate_Serializer
import com.ing.zkflow.Party_EdDSA
import com.ing.zkflow.Party_EdDSA_Serializer
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.CordaX500NameSpec
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer
import com.ing.zkflow.serialization.serializer.corda.PartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.PublicKey

class PartiesTest : SerializerTest {
    // Setup
    @ZKP
    data class Parties(
        val anonymousParty: @EdDSA AnonymousParty = someAnonymous,
        val anonymousPartyFullyCustom: @Via<AnonymousParty_EdDSA> AnonymousParty = someAnonymous,

        val party: @EdDSA Party = someParty,
        val partyCX500Custom: @EdDSA @CordaX500NameSpec<CordaX500NameSurrogate>(CordaX500NameConverter::class) Party = someParty,
        val partyFullyCustom: @Via<Party_EdDSA> Party = someParty,
    ) {
        companion object {
            private val pk: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
            val someAnonymous = AnonymousParty(pk)
            val someParty = Party(CordaX500Name(organisation = "IN", locality = "AMS", country = "NL"), pk)
        }
    }

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class PartiesResolved constructor (
        @Serializable(with = AnonymousParty_0::class) val anonymousParty: @Contextual AnonymousParty = someAnonymous,
        @Serializable(with = AnonymousPartyFullyCustom_0::class) val anonymousPartyFullyCustom: AnonymousParty = someAnonymous,
        @Serializable(with = Party_0::class) val party: @EdDSA @Contextual Party = someParty,
        @Serializable(with = PartyCX500Custom_0::class) val partyCX500Custom: @Contextual Party = someParty,
        @Serializable(with = PartyFullyCustom_0::class) val partyFullyCustom: @Contextual Party = someParty
    ) {

        companion object {
            private val pk: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
            val someAnonymous = AnonymousParty(pk)
            val someParty = Party(CordaX500Name(organisation = "IN", locality = "AMS", country = "NL"), pk)
        }

        object AnonymousParty_0 : AnonymousPartySerializer(4)
        object AnonymousPartyFullyCustom_0 : WrappedFixedLengthKSerializer<AnonymousParty>(
            AnonymousParty_EdDSA_Serializer,
            AnonymousParty_EdDSA::class.java.isEnum
        )

        object Party_0 : PartySerializer(4, Party_1)
        object Party_1 : WrappedFixedLengthKSerializerWithDefault<CordaX500Name>(CordaX500NameSerializer)

        object PartyCX500Custom_0 : PartySerializer(4, PartyCX500Custom_1)
        object PartyCX500Custom_1 : SerializerWithDefault<CordaX500Name>(
            CordaX500NameSurrogate_Serializer,
            CordaX500NameSerializer.default
        )

        object PartyFullyCustom_0 : WrappedFixedLengthKSerializer<Party>(
            Party_EdDSA_Serializer,
            Party_EdDSA::class.java.isEnum
        )
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `Parties makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(PartiesTest_Parties_Serializer, Parties())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Parties generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            PartiesResolved.serializer(),
            PartiesResolved()
        ) shouldBe
            engine.serialize(PartiesTest_Parties_Serializer, Parties())
    }
}
