package io.ivno.collateraltoken.workflow.deposit

import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.DepositSchema.DepositEntity
import io.ivno.collateraltoken.contract.DepositStatus
import io.onixlabs.corda.core.workflow.DEFAULT_PAGE_SPECIFICATION
import io.onixlabs.corda.core.workflow.FindStateFlow
import io.onixlabs.corda.core.workflow.andWithExpressions
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import java.math.BigDecimal
import java.time.Instant

@StartableByRPC
class FindDepositFlow(
    linearId: UniqueIdentifier? = null,
    externalId: String? = null,
    depositor: AbstractParty? = null,
    custodian: AbstractParty? = null,
    tokenIssuingEntity: AbstractParty? = null,
    amount: BigDecimal? = null,
    tokenTypeLinearId: UniqueIdentifier? = null,
    tokenTypeExternalId: String? = null,
    reference: String? = null,
    depositStatus: DepositStatus? = null,
    timestamp: Instant? = null,
    accountId: String? = null,
    stateStatus: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
    relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
    override val pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION
) : FindStateFlow<Deposit>() {
    override val criteria: QueryCriteria = VaultQueryCriteria(
        contractStateTypes = setOf(contractStateType),
        relevancyStatus = relevancyStatus,
        status = stateStatus
    ).andWithExpressions(
        linearId?.let { DepositEntity::linearId.equal(it.id) },
        externalId?.let { DepositEntity::externalId.equal(it) },
        depositor?.let { DepositEntity::depositor.equal(it) },
        custodian?.let { DepositEntity::custodian.equal(it) },
        tokenIssuingEntity?.let { DepositEntity::tokenIssuingEntity.equal(it) },
        amount?.let { DepositEntity::amount.equal(it) },
        tokenTypeLinearId?.let { DepositEntity::tokenTypeLinearId.equal(it.id) },
        tokenTypeExternalId?.let { DepositEntity::tokenTypeExternalId.equal(it) },
        reference?.let { DepositEntity::reference.equal(it) },
        depositStatus?.let { DepositEntity::status.equal(it) },
        timestamp?.let { DepositEntity::timestamp.equal(it) },
        accountId?.let { DepositEntity::accountId.equal(it) }
    )
}
