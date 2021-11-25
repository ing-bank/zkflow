package com.ing.zkflow.annotated.ivno

import com.ing.zkflow.ASCII
import com.ing.zkflow.Converter
import com.ing.zkflow.ZKP
import com.ing.zkflow.annotated.ivno.deps.BigDecimalAmount
import com.ing.zkflow.annotated.ivno.deps.Network
import com.ing.zkflow.annotated.ivno.infra.BigDecimalAmountConverter_LinearPointer_IvnoTokenType
import com.ing.zkflow.annotated.ivno.infra.BigDecimalAmountSurrogate_LinearPointer_IvnoTokenType
import com.ing.zkflow.annotated.ivno.infra.EdDSAParty
import com.ing.zkflow.annotated.ivno.infra.EdDSAPartyConverter
import com.ing.zkflow.annotated.ivno.infra.InstantConverter
import com.ing.zkflow.annotated.ivno.infra.InstantSurrogate
import com.ing.zkflow.annotated.ivno.infra.NetworkAnonymousOperatorConverter
import com.ing.zkflow.annotated.ivno.infra.NetworkEdDSAAnonymousOperator
import com.ing.zkflow.annotated.ivno.infra.UniqueIdentifierConverter
import com.ing.zkflow.annotated.ivno.infra.UniqueIdentifierSurrogate
import kotlinx.serialization.Transient
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant

@ZKP
data class IvnoDeposit constructor(
    val depositor: @Converter<Party, EdDSAParty>(EdDSAPartyConverter::class) Party,
    val custodian: @Converter<Party, EdDSAParty>(EdDSAPartyConverter::class) Party,
    val tokenIssuingEntity: @Converter<Party, EdDSAParty>(EdDSAPartyConverter::class) Party,
    val amount:
        @Converter<BigDecimalAmount<LinearPointer<IvnoTokenType>>, BigDecimalAmountSurrogate_LinearPointer_IvnoTokenType>(
            BigDecimalAmountConverter_LinearPointer_IvnoTokenType::class
        ) BigDecimalAmount<LinearPointer<IvnoTokenType>>,
    //
    val reference: @ASCII(10) String?,
    val status: DepositStatus,
    val timestamp: @Converter<Instant, InstantSurrogate>(InstantConverter::class) Instant,
    val accountId: @ASCII(10) String,
    val linearId: @Converter<UniqueIdentifier, UniqueIdentifierSurrogate>(UniqueIdentifierConverter::class) UniqueIdentifier
)

@ZKP
enum class DepositStatus {
    DEPOSIT_REQUESTED,
    DEPOSIT_ACCEPTED,
    DEPOSIT_REJECTED,
    DEPOSIT_CANCELLED,
    PAYMENT_ISSUED,
    PAYMENT_ACCEPTED,
    PAYMENT_REJECTED,
}

@ZKP
data class IvnoTokenType(
    val network: @Converter<Network, NetworkEdDSAAnonymousOperator>(NetworkAnonymousOperatorConverter::class) Network,
    val custodian: @Converter<Party, EdDSAParty>(EdDSAPartyConverter::class) Party,
    val tokenIssuingEntity: @Converter<Party, EdDSAParty>(EdDSAPartyConverter::class) Party,
    val displayName: @ASCII(10) String,
    val fractionDigits: Int = 0,
    override val linearId: @Converter<UniqueIdentifier, UniqueIdentifierSurrogate>(UniqueIdentifierConverter::class) UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    @Transient
    override val participants: List<AbstractParty> = setOf(tokenIssuingEntity, custodian).toList()
}
