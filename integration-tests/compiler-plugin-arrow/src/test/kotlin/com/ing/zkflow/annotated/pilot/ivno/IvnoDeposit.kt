package com.ing.zkflow.annotated.pilot.ivno

import com.ing.zkflow.ASCII
import com.ing.zkflow.Converter
import com.ing.zkflow.ZKP
import com.ing.zkflow.annotated.pilot.infra.BigDecimalAmountConverter_LinearPointer_IvnoTokenType
import com.ing.zkflow.annotated.pilot.infra.BigDecimalAmountSurrogate_LinearPointer_IvnoTokenType
import com.ing.zkflow.annotated.pilot.infra.EdDSAParty
import com.ing.zkflow.annotated.pilot.infra.EdDSAPartyConverter
import com.ing.zkflow.annotated.pilot.infra.NetworkAnonymousOperatorConverter
import com.ing.zkflow.annotated.pilot.infra.NetworkEdDSAAnonymousOperator
import com.ing.zkflow.annotated.pilot.infra.UniqueIdentifierConverter
import com.ing.zkflow.annotated.pilot.infra.UniqueIdentifierSurrogate
import com.ing.zkflow.annotated.pilot.ivno.deps.BigDecimalAmount
import com.ing.zkflow.annotated.pilot.ivno.deps.Network
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
    val timestamp: Instant,
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
    override val participants: List<AbstractParty> = setOf(tokenIssuingEntity, custodian).toList()

    val testFieldWithGetter: Int get() = 0
    val testFieldNoGetter: Int = 0
}
