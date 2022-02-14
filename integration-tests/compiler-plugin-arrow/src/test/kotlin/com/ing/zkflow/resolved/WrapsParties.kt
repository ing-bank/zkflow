package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.pilot.infra.AnonymousPartyConverter_EdDSA
import com.ing.zkflow.annotated.pilot.infra.CordaX500NameConverter
import com.ing.zkflow.annotated.pilot.infra.CordaX500NameSurrogate
import com.ing.zkflow.annotated.pilot.infra.EdDSAPartyConverter
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.PublicKey

@Suppress("ClassName")
@Serializable
data class WrapsParties constructor (
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

    object AnonymousParty_0 : com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer(4)
    object AnonymousPartyFullyCustom_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<AnonymousParty, com.ing.zkflow.annotated.pilot.infra.AnonymousPartySurrogate_EdDSA>(
        com.ing.zkflow.annotated.pilot.infra.AnonymousPartySurrogate_EdDSA.serializer(), { AnonymousPartyConverter_EdDSA.from(it) }
    )
    object Party_0 : com.ing.zkflow.serialization.serializer.corda.PartySerializer(4, Party_1)
    object Party_1 : com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault<CordaX500Name>(com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer)
    object PartyCX500Custom_0 : com.ing.zkflow.serialization.serializer.corda.PartySerializer(4, PartyCX500Custom_1)
    object PartyCX500Custom_1 : com.ing.zkflow.serialization.serializer.SerializerWithDefault<CordaX500Name>(PartyCX500Custom_2, com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer.default)
    object PartyCX500Custom_2 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<CordaX500Name, CordaX500NameSurrogate>(
        CordaX500NameSurrogate.serializer(), { CordaX500NameConverter.from(it) }
    )
    object PartyFullyCustom_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<Party, com.ing.zkflow.annotated.pilot.infra.EdDSAParty>(
        com.ing.zkflow.annotated.pilot.infra.EdDSAParty.serializer(), { EdDSAPartyConverter.from(it) }
    )
}

class WrapsPartiesTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsParties make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsParties.serializer(),
            com.ing.zkflow.annotated.WrapsParties()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsParties generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsParties.serializer(),
            com.ing.zkflow.annotated.WrapsParties()
        ) shouldBe
            engine.serialize(WrapsParties.serializer(), WrapsParties())
    }
}
