@file:Suppress("ClassName")

package io.ivno.resolved

import com.ing.zkflow.fixedCordaX500Name
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.testing.zkp.ZKNulls.fixedKeyPair
import io.ivno.annotated.DepositStatus
import io.ivno.annotated.IvnoTokenType
import io.ivno.annotated.deps.BigDecimalAmount
import io.ivno.annotated.fixtures.BigDecimalAmount_LinearPointer_IvnoTokenType
import io.ivno.annotated.fixtures.BigDecimalAmount_LinearPointer_IvnoTokenType_Converter
import io.ivno.annotated.fixtures.UniqueIdentifierConverter
import io.ivno.annotated.fixtures.UniqueIdentifierSurrogate
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.Crypto
import net.corda.core.identity.CordaX500Name
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
    object Depositor_0 : com.ing.zkflow.serialization.serializer.corda.PartySerializer(4, Depositor_1)
    object Depositor_1 : WrappedFixedLengthKSerializerWithDefault<CordaX500Name>(com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer)

    object Custodian_0 : com.ing.zkflow.serialization.serializer.corda.PartySerializer(4, Custodian_1)
    object Custodian_1 : WrappedFixedLengthKSerializerWithDefault<CordaX500Name>(com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer)

    object TokenIssuingEntity_0 : com.ing.zkflow.serialization.serializer.corda.PartySerializer(4, TokenIssuingEntity_1)
    object TokenIssuingEntity_1 : WrappedFixedLengthKSerializerWithDefault<CordaX500Name>(com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer)

    object Amount_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<BigDecimalAmount<LinearPointer<IvnoTokenType>>, BigDecimalAmount_LinearPointer_IvnoTokenType>(
        BigDecimalAmount_LinearPointer_IvnoTokenType.serializer(),
        { BigDecimalAmount_LinearPointer_IvnoTokenType_Converter.from(it) }
    )

    object Reference_0 : com.ing.zkflow.serialization.serializer.NullableSerializer<String>(Reference_1)

    object Reference_1 : com.ing.zkflow.serialization.serializer.string.FixedLengthStringSerializer(10)

    object Status_0 : com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer<DepositStatus>(DepositStatus.serializer(), DepositStatus::class.java.isEnum)

    object Timestamp_0 : WrappedFixedLengthKSerializerWithDefault<Instant>(com.ing.zkflow.serialization.serializer.InstantSerializer)

    object AccountId_0 : com.ing.zkflow.serialization.serializer.string.FixedLengthStringSerializer(10)

    object LinearId_0 : com.ing.zkflow.serialization.serializer.SurrogateSerializer<UniqueIdentifier, UniqueIdentifierSurrogate>(
        UniqueIdentifierSurrogate.serializer(), { UniqueIdentifierConverter.from(it) }
    )
}

class IvnoDepositTest : SerializerTest {
    private val publicKey: PublicKey = fixedKeyPair(Crypto.EDDSA_ED25519_SHA512).public

    private val party = Party(fixedCordaX500Name, publicKey)

    private val uuid = UniqueIdentifier()

    private val deposit = io.ivno.annotated.IvnoDeposit(
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
        engine.assertRoundTrip(io.ivno.annotated.IvnoDeposit.serializer(), deposit)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `IvnoDeposit generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(io.ivno.annotated.IvnoDeposit.serializer(), deposit) shouldBe
            engine.serialize(IvnoDeposit.serializer(), resolvedDeposit)
    }
}
