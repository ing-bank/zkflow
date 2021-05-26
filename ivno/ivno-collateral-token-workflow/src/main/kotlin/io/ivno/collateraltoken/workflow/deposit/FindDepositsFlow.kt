package io.ivno.collateraltoken.workflow.deposit

import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.DepositSchema
import io.ivno.collateraltoken.contract.DepositStatus
import io.onixlabs.corda.core.workflow.DEFAULT_PAGE_SPECIFICATION
import io.onixlabs.corda.core.workflow.DEFAULT_SORTING
import io.onixlabs.corda.core.workflow.FindStatesFlow
import io.onixlabs.corda.core.workflow.andWithExpressions
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.node.services.vault.Sort
import java.math.BigDecimal
import java.time.Instant

@StartableByRPC
class FindDepositsFlow(
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
    override val pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION,
    override val sorting: Sort = DEFAULT_SORTING
) : FindStatesFlow<Deposit>() {
    override val criteria: QueryCriteria = VaultQueryCriteria(
        contractStateTypes = setOf(contractStateType),
        relevancyStatus = relevancyStatus,
        status = stateStatus
    ).andWithExpressions(
        linearId?.let { DepositSchema.DepositEntity::linearId.equal(it.id) },
        externalId?.let { DepositSchema.DepositEntity::externalId.equal(it) },
        depositor?.let { DepositSchema.DepositEntity::depositor.equal(it) },
        custodian?.let { DepositSchema.DepositEntity::custodian.equal(it) },
        tokenIssuingEntity?.let { DepositSchema.DepositEntity::tokenIssuingEntity.equal(it) },
        amount?.let { DepositSchema.DepositEntity::amount.equal(it) },
        tokenTypeLinearId?.let { DepositSchema.DepositEntity::tokenTypeLinearId.equal(it.id) },
        tokenTypeExternalId?.let { DepositSchema.DepositEntity::tokenTypeExternalId.equal(it) },
        reference?.let { DepositSchema.DepositEntity::reference.equal(it) },
        depositStatus?.let { DepositSchema.DepositEntity::status.equal(it) },
        timestamp?.let { DepositSchema.DepositEntity::timestamp.equal(it) },
        accountId?.let { DepositSchema.DepositEntity::accountId.equal(it) }
    )
}
