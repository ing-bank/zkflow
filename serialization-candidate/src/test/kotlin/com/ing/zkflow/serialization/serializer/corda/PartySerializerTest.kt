package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.serialization.serializer.WrappedKSerializerWithDefault
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class PartySerializerTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `Party must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(Parties.Party_0, Parties.someParty)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Party's must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(Parties.Party_0, Parties.someParty).size shouldBe
            engine.serialize(Parties.Party_0, Parties.otherParty).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with Party must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(Parties.serializer(), Parties.containerParties)
    }

    @Suppress("ClassName")
    @Serializable
    data class Parties(
        @Serializable(with = Party_0::class) val party: Party,
        @Serializable(with = CustomParty_0::class) val customParty: Party,
    ) {
        object Party_0 : PartySerializer(scheme.schemeNumberID, Party_1)
        object Party_1 : WrappedKSerializerWithDefault<CordaX500Name>(CordaX500NameSerializer)

        object CustomParty_0 : PartySerializer(scheme.schemeNumberID, CustomParty_1)
        object CustomParty_1 : SerializerWithDefault<CordaX500Name>(CustomParty_2, CordaX500NameSerializer.default)
        object CustomParty_2 : SurrogateSerializer<CordaX500Name, CordaX500NameSurrogate>(
            CordaX500NameSurrogate.serializer(), { CordaX500NameConverter.from(it) }
        )

        companion object {
            private val scheme = Crypto.EDDSA_ED25519_SHA512
            val someParty = Party(CordaX500Name(organisation = "IN", locality = "AMS", country = "NL"), Crypto.generateKeyPair(scheme).public)
            val otherParty = Party(CordaX500Name(organisation = "IN", locality = "AMS", country = "NL"), Crypto.generateKeyPair(scheme).public)
            val containerParties = Parties(someParty, otherParty)
        }
    }
}
