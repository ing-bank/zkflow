@file:Suppress("ClassName")

package com.ing.zkflow.resolved.pilot.ivno

import com.ing.zkflow.annotated.pilot.infra.BigDecimalAmountConverter_LinearPointer_IvnoTokenType
import com.ing.zkflow.annotated.pilot.infra.BigDecimalAmountSurrogate_LinearPointer_IvnoTokenType
import com.ing.zkflow.annotated.pilot.infra.EdDSAParty
import com.ing.zkflow.annotated.pilot.infra.EdDSAPartyConverter
import com.ing.zkflow.annotated.pilot.infra.UniqueIdentifierConverter
import com.ing.zkflow.annotated.pilot.infra.UniqueIdentifierSurrogate
import com.ing.zkflow.annotated.pilot.infra.fixedCordaX500Name
import com.ing.zkflow.annotated.pilot.ivno.DepositStatus
import com.ing.zkflow.annotated.pilot.ivno.IvnoTokenType
import com.ing.zkflow.annotated.pilot.ivno.deps.BigDecimalAmount
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.testing.zkp.ZKNulls.fixedKeyPair
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.Crypto
import net.corda.core.identity.Party
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Instant

@Serializable
data class IvnoDeposit(
    @Serializable(with = Depositor_0::class) val depositor: @Contextual Party,
    @Serializable(with = Custodian_0::class) val custodian: @Contextual Party,
    @Serializable(with = TokenIssuingEntity_0::class) val tokenIssuingEntity: @Contextual Party,
    @Serializable(with = Amount_0::class) val amount: BigDecimalAmount<@Contextual LinearPointer<@Contextual IvnoTokenType>>,
    @Serializable(with = Reference_0::class) val reference: @Contextual String?,
    @Serializable(with = Status_0::class) val status: @Contextual DepositStatus,
    @Serializable(with = Timestamp_0::class) val timestamp: @Contextual Instant,
    @Serializable(with = AccountId_0::class) val accountId: @Contextual String,
    @Serializable(with = LinearId_0::class) val linearId: @Contextual UniqueIdentifier
) {
    object Depositor_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<Party, EdDSAParty>(
        EdDSAParty.serializer(), { EdDSAPartyConverter.from(it) }
    )

    object Custodian_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<Party, EdDSAParty>(
        EdDSAParty.serializer(), { EdDSAPartyConverter.from(it) }
    )

    object TokenIssuingEntity_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<Party, EdDSAParty>(
        EdDSAParty.serializer(), { EdDSAPartyConverter.from(it) }
    )

    object Amount_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<BigDecimalAmount<LinearPointer<IvnoTokenType>>, BigDecimalAmountSurrogate_LinearPointer_IvnoTokenType>(
        BigDecimalAmountSurrogate_LinearPointer_IvnoTokenType.serializer(),
        { BigDecimalAmountConverter_LinearPointer_IvnoTokenType.from(it) }
    )

    object Reference_0 : com.ing.zkflow.serialization.serializer.NullableSerializer<String>(Reference_1)

    object Reference_1 : com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer(10)

    object Status_0 : com.ing.zkflow.serialization.serializer.WrappedKSerializer<DepositStatus>(DepositStatus.serializer())

    object Timestamp_0 : com.ing.zkflow.serialization.serializer.WrappedKSerializer<Instant>(com.ing.zkflow.serialization.serializer.InstantSerializer)

    object AccountId_0 : com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer(10)

    object LinearId_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<UniqueIdentifier, UniqueIdentifierSurrogate>(
        UniqueIdentifierSurrogate.serializer(), { UniqueIdentifierConverter.from(it) }
    )
}

class IvnoDepositTest : SerializerTest {
    private val publicKey: PublicKey = fixedKeyPair(Crypto.EDDSA_ED25519_SHA512).public

    private val party = Party(fixedCordaX500Name, publicKey)

    private val uuid = UniqueIdentifier()

    private val deposit = com.ing.zkflow.annotated.pilot.ivno.IvnoDeposit(
        depositor = party,
        custodian = party,
        tokenIssuingEntity = party,
        amount = BigDecimalAmount(BigDecimal.ONE, LinearPointer(uuid, IvnoTokenType::class.java)),
        reference = null,
        status = DepositStatus.PAYMENT_ISSUED,
        timestamp = Instant.MIN,
        accountId = "x",
        linearId = uuid
    )

    private val resolvedDeposit = with(deposit) {
        IvnoDeposit(
            depositor,
            custodian,
            tokenIssuingEntity,
            amount,
            reference,
            status,
            timestamp,
            accountId,
            linearId
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `IvnoDeposit makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(com.ing.zkflow.annotated.pilot.ivno.IvnoDeposit.serializer(), deposit)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `IvnoDeposit generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(com.ing.zkflow.annotated.pilot.ivno.IvnoDeposit.serializer(), deposit) shouldBe
            engine.serialize(IvnoDeposit.serializer(), resolvedDeposit)
    }
}
