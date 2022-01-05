@file:Suppress("ClassName")

package com.ing.zkflow.resolved.pilot.r3.states

import com.ing.zkflow.annotated.pilot.infra.AmountConverter_IssuedTokenType
import com.ing.zkflow.annotated.pilot.infra.AmountSurrogate_IssuedTokenType
import com.ing.zkflow.annotated.pilot.infra.EdDSAParty
import com.ing.zkflow.annotated.pilot.infra.EdDSAPartyConverter
import com.ing.zkflow.annotated.pilot.infra.fixedCordaX500Name
import com.ing.zkflow.annotated.pilot.r3.states.AbstractFungibleToken
import com.ing.zkflow.annotated.pilot.r3.types.IssuedTokenType
import com.ing.zkflow.annotated.pilot.r3.types.TokenType
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.testing.zkp.ZKNulls.fixedKeyPair
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.Amount
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.PublicKey
import java.time.Instant

@Serializable
data class CBDCToken(
    @Serializable(with = Amount_0::class) override val amount: @Contextual Amount<@Contextual IssuedTokenType>,
    @Serializable(with = Holder_0::class) override val holder: @Contextual Party,
    @Serializable(with = TokenTypeJarHash_0::class) override val tokenTypeJarHash: @Contextual SecureHash? = SecureHash.zeroHash,
    @Serializable(with = IssueDate_0::class) val issueDate: @Contextual Instant = Instant.now(),
    @Serializable(with = LastInterestAccrualDate_0::class) val lastInterestAccrualDate: @Contextual Instant = issueDate,
    @Serializable(with = UsageCount_0::class) val usageCount: @Contextual Int = 0
) : AbstractFungibleToken() { // , ReissuableState<CBDCToken>
    override fun withNewHolder(newHolder: Party): AbstractFungibleToken {
        return CBDCToken(amount, newHolder, tokenTypeJarHash = tokenTypeJarHash)
    }

    object Amount_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<Amount<IssuedTokenType>, AmountSurrogate_IssuedTokenType>(
        AmountSurrogate_IssuedTokenType.serializer(),
        { AmountConverter_IssuedTokenType.from(it) }
    )

    object Holder_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<Party, EdDSAParty>(
        EdDSAParty.serializer(),
        { EdDSAPartyConverter.from(it) }
    )

    object TokenTypeJarHash_0 : com.ing.zkflow.serialization.serializer.NullableSerializer<SecureHash>(
        TokenTypeJarHash_1
    )

    object TokenTypeJarHash_1 : com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer("Sha256", 32)

    object IssueDate_0 : com.ing.zkflow.serialization.serializer.WrappedKSerializer<Instant>(com.ing.zkflow.serialization.serializer.InstantSerializer)

    object LastInterestAccrualDate_0 : com.ing.zkflow.serialization.serializer.WrappedKSerializer<Instant>(com.ing.zkflow.serialization.serializer.InstantSerializer)

    object UsageCount_0 : com.ing.zkflow.serialization.serializer.WrappedKSerializerWithDefault<Int>(com.ing.zkflow.serialization.serializer.IntSerializer)
}

class CBDCTokenTest : SerializerTest {
    private val publicKey: PublicKey = fixedKeyPair(Crypto.EDDSA_ED25519_SHA512).public

    private val party = Party(fixedCordaX500Name, publicKey)

    private val cbdcToken = com.ing.zkflow.annotated.pilot.r3.states.CBDCToken(
        amount = Amount(
            100,
            IssuedTokenType(
                issuer = party,
                tokenType = TokenType(
                    tokenIdentifier = "FAKE",
                    fractionDigits = 2,
                )
            )
        ),
        holder = party,
    )

    private val resolvedCBDCToken = with(cbdcToken) {
        CBDCToken(amount, holder, tokenTypeJarHash, issueDate, lastInterestAccrualDate, usageCount)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `CBDCToken makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(com.ing.zkflow.annotated.pilot.r3.states.CBDCToken.serializer(), cbdcToken)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `CBDCToken generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(com.ing.zkflow.annotated.pilot.r3.states.CBDCToken.serializer(), cbdcToken) shouldBe
            engine.serialize(CBDCToken.serializer(), resolvedCBDCToken)
    }
}
