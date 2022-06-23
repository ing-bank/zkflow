package io.ivno.annotated

import com.ing.zkflow.Via
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import io.ivno.annotated.deps.BigDecimalAmount
import io.ivno.annotated.deps.Network
import io.ivno.annotated.fixtures.BigDecimalAmount_LinearPointer_IvnoTokenType
import io.ivno.annotated.fixtures.NetworkEdDSAAnonymousOperator
import io.ivno.annotated.fixtures.UniqueIdentifierSurrogate
import net.corda.core.contracts.ContractState
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
    val reference: @UTF8(10) String?,
    val status: DepositStatus,
    val timestamp: Instant,
    val accountId: @UTF8(10) String,
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

interface VersionedIvnotokenType : VersionedContractStateGroup, ContractState

@ZKP
data class IvnoTokenType(
    val network: @Via<NetworkEdDSAAnonymousOperator> Network,
    val custodian: @EdDSA Party,
    val tokenIssuingEntity: @EdDSA Party,
    val displayName: @UTF8(10) String,
    val fractionDigits: Int = 0,
    val uniqueId: @Via<UniqueIdentifierSurrogate> UniqueIdentifier = UniqueIdentifier(),
) : LinearState, VersionedIvnotokenType {
    override val linearId = uniqueId
    override val participants: List<AbstractParty> = setOf(tokenIssuingEntity, custodian).toList()
}
