@file:Suppress("ClassName")

package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.AmountSurrogate_IssuedTokenTypeV1Serializer
import com.ing.zkflow.annotated.ExampleTokenSerializer
import com.ing.zkflow.fixedCordaX500Name
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.InstantSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.testing.zkp.ZKNulls.fixedKeyPair
import com.r3.corda.lib.tokens.states.AbstractFungibleToken
import com.r3.corda.lib.tokens.types.IssuedTokenType
import com.r3.corda.lib.tokens.types.TokenType
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.Amount
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.PublicKey
import java.time.Instant

@Serializable
data class ExampleToken(
    @Serializable(with = Amount_0::class) val myAmount: @Contextual Amount<@Contextual IssuedTokenType>,
    @Serializable(with = Holder_0::class) val owner: @Contextual AnonymousParty,
    @Serializable(with = IssueDate_0::class) val issueDate: @Contextual Instant = Instant.now(),
    @Serializable(with = LastInterestAccrualDate_0::class) val lastInterestAccrualDate: @Contextual Instant = issueDate,
    @Serializable(with = UsageCount_0::class) val usageCount: @Contextual Int = 0
) : AbstractFungibleToken() {

    @Transient
    override val amount: Amount<IssuedTokenType> = myAmount

    @Transient
    override val holder: AnonymousParty = owner

    @Transient
    override val tokenTypeJarHash: SecureHash = SecureHash.zeroHash

    override fun withNewHolder(newHolder: AbstractParty): AbstractFungibleToken {
        require(newHolder is AnonymousParty)
        return ExampleToken(amount, newHolder)
    }

    object Amount_0 : WrappedFixedLengthKSerializer<Amount<IssuedTokenType>>(AmountSurrogate_IssuedTokenTypeV1Serializer, Amount::class.java.isEnum)

    object Holder_0 : com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer(4)

    object IssueDate_0 : WrappedFixedLengthKSerializerWithDefault<Instant>(InstantSerializer)

    object LastInterestAccrualDate_0 :
        WrappedFixedLengthKSerializerWithDefault<Instant>(InstantSerializer)

    object UsageCount_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
}

class ExampleTokenTest : SerializerTest {
    private val publicKey: PublicKey = fixedKeyPair(Crypto.EDDSA_ED25519_SHA512).public

    private val party = Party(fixedCordaX500Name, publicKey)

    private val exampleToken = com.ing.zkflow.annotated.ExampleToken(
        myAmount = Amount(
            100,
            IssuedTokenType(
                issuer = party,
                tokenType = TokenType(
                    tokenIdentifier = "FAKE",
                    fractionDigits = 2,
                )
            )
        ),
        owner = party.anonymise(),
    )

    private val resolvedExampleToken = with(exampleToken) {
        ExampleToken(amount, owner, issueDate, lastInterestAccrualDate, usageCount)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ExampleToken makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(ExampleTokenSerializer, exampleToken)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ExampleToken generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(ExampleTokenSerializer, exampleToken) shouldBe
            engine.serialize(ExampleToken.serializer(), resolvedExampleToken)
    }
}
