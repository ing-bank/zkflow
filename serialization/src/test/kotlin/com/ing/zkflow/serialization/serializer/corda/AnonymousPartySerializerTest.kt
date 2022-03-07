package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class AnonymousPartySerializerTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `AnonymousParty must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsAnonymousParty.AnonymousParty_0, ContainsAnonymousParty.someAnonymousParty)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `AnonymousParty's must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(ContainsAnonymousParty.AnonymousParty_0, ContainsAnonymousParty.someAnonymousParty).size shouldBe
            engine.serialize(ContainsAnonymousParty.AnonymousParty_0, ContainsAnonymousParty.otherAnonymousParty).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with AnonymousParty must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsAnonymousParty.serializer(), ContainsAnonymousParty.containerAnonymousParty)
    }

    @Suppress("ClassName")
    @Serializable
    data class ContainsAnonymousParty(
        @Serializable(with = AnonymousParty_0::class) val anonymousParty: AnonymousParty,
    ) {
        object AnonymousParty_0 : AnonymousPartySerializer(scheme.schemeNumberID)

        companion object {
            private val scheme = Crypto.EDDSA_ED25519_SHA512
            val someAnonymousParty = AnonymousParty(Crypto.generateKeyPair(scheme).public)
            val otherAnonymousParty = AnonymousParty(Crypto.generateKeyPair(scheme).public)
            val containerAnonymousParty = ContainsAnonymousParty(someAnonymousParty)
        }
    }
}
