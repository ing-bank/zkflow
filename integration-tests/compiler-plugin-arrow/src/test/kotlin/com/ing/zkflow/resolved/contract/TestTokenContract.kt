package com.ing.zkflow.resolved.contract

import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.WrappedKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.CommandAndState
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@Serializable
data class TestTokenState(
    @Serializable(with = Owner_0::class) override val owner: @Contextual AnonymousParty =
        AnonymousParty(PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)),
    @Serializable(with = Int_0::class) val int: @Contextual Int = 1877,
    @Serializable(with = String_0::class) val string: @Contextual String = "abc"
) : ZKOwnableState {

    @Transient
    override val participants: List<AnonymousParty> = listOf(owner)

    override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
        TODO("Not yet implemented")

    object Owner_0 : AnonymousPartySerializer(4)
    object Int_0 : WrappedKSerializerWithDefault<Int>(com.ing.zkflow.serialization.serializer.IntSerializer)
    object String_0 : FixedLengthASCIIStringSerializer(10)
}

class TestTokenStateTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `TestTokenState make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.contract.TestTokenContract.TestTokenState.serializer(),
            com.ing.zkflow.annotated.contract.TestTokenContract.TestTokenState()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `TestTokenState generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.contract.TestTokenContract.TestTokenState.serializer(),
            com.ing.zkflow.annotated.contract.TestTokenContract.TestTokenState()
        ) shouldBe
            engine.serialize(TestTokenState.serializer(), TestTokenState())
    }
}
