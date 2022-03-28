package io.ivno.annotated

import com.ing.zkflow.Via
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import io.ivno.annotated.deps.BigDecimalAmount
import io.ivno.annotated.deps.Network
import io.ivno.annotated.fixtures.BigDecimalAmount_LinearPointer_IvnoTokenType
import io.ivno.annotated.fixtures.NetworkEdDSAAnonymousOperator
import io.ivno.annotated.fixtures.UniqueIdentifierSurrogate
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant

@ZKP
data class IvnoDeposit constructor(
    val depositor: @EdDSA Party,
    val custodian: @EdDSA Party,
    val tokenIssuingEntity: @EdDSA Party,
    val amount: @Via<BigDecimalAmount_LinearPointer_IvnoTokenType> BigDecimalAmount<LinearPointer<IvnoTokenType>>,
    //
    val reference: @Size(10) String?,
    val status: DepositStatus,
    val timestamp: Instant,
    val accountId: @Size(10) String,
    val linearId: @Via<UniqueIdentifierSurrogate> UniqueIdentifier
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
    val network: @Via<NetworkEdDSAAnonymousOperator> Network,
    val custodian: @EdDSA Party,
    val tokenIssuingEntity: @EdDSA Party,
    val displayName: @Size(10) String,
    val fractionDigits: Int = 0,
    override val linearId: @Via<UniqueIdentifierSurrogate> UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty> = setOf(tokenIssuingEntity, custodian).toList()
}
