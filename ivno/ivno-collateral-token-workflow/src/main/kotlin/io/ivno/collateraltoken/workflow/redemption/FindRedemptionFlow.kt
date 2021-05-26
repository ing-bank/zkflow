package io.ivno.collateraltoken.workflow.redemption

import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.contract.RedemptionSchema.RedemptionEntity
import io.ivno.collateraltoken.contract.RedemptionStatus
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
class FindRedemptionFlow(
    linearId: UniqueIdentifier? = null,
    externalId: String? = null,
    redeemer: AbstractParty? = null,
    custodian: AbstractParty? = null,
    amount: BigDecimal? = null,
    tokenTypeLinearId: UniqueIdentifier? = null,
    tokenTypeExternalId: String? = null,
    redemptionStatus: RedemptionStatus? = null,
    timestamp: Instant? = null,
    accountId: String? = null,
    stateStatus: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
    relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
    override val pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION
) : FindStateFlow<Redemption>() {
    override val criteria: QueryCriteria = VaultQueryCriteria(
        contractStateTypes = setOf(contractStateType),
        relevancyStatus = relevancyStatus,
        status = stateStatus
    ).andWithExpressions(
        linearId?.let { RedemptionEntity::linearId.equal(it.id) },
        externalId?.let { RedemptionEntity::externalId.equal(it) },
        redeemer?.let { RedemptionEntity::redeemer.equal(it) },
        custodian?.let { RedemptionEntity::custodian.equal(it) },
        amount?.let { RedemptionEntity::amount.equal(it) },
        tokenTypeLinearId?.let { RedemptionEntity::tokenTypeLinearId.equal(it.id) },
        tokenTypeExternalId?.let { RedemptionEntity::tokenTypeExternalId.equal(it) },
        redemptionStatus?.let { RedemptionEntity::status.equal(it) },
        timestamp?.let { RedemptionEntity::timestamp.equal(it) },
        accountId?.let { RedemptionEntity::accountId.equal(it) }
    )
}
