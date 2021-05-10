package io.ivno.collateraltoken.integration

import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.contract.RedemptionStatus
import io.ivno.collateraltoken.workflow.redemption.FindRedemptionFlow
import io.ivno.collateraltoken.workflow.redemption.FindRedemptionsFlow
import io.onixlabs.corda.core.integration.RPCService
import io.onixlabs.corda.core.workflow.DEFAULT_PAGE_SPECIFICATION
import io.onixlabs.corda.core.workflow.DEFAULT_SORTING
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.Sort
import net.corda.core.utilities.getOrThrow
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class RedemptionQueryService(rpc: CordaRPCOps) : RPCService(rpc) {

    fun findRedemptions(
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
        pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION,
        sorting: Sort = DEFAULT_SORTING,
        timeout: Duration = Duration.ofSeconds(30)
    ): List<StateAndRef<Redemption>> {
        return rpc.startFlowDynamic(
            FindRedemptionsFlow::class.java,
            linearId,
            externalId,
            redeemer,
            custodian,
            amount,
            tokenTypeLinearId,
            tokenTypeExternalId,
            redemptionStatus,
            timestamp,
            accountId,
            stateStatus,
            relevancyStatus,
            pageSpecification,
            sorting
        ).returnValue.getOrThrow(timeout)
    }

    fun findRedemption(
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
        pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION,
        timeout: Duration = Duration.ofSeconds(30)
    ): StateAndRef<Redemption>? {
        return rpc.startFlowDynamic(
            FindRedemptionFlow::class.java,
            linearId,
            externalId,
            redeemer,
            custodian,
            amount,
            tokenTypeLinearId,
            tokenTypeExternalId,
            redemptionStatus,
            timestamp,
            accountId,
            stateStatus,
            relevancyStatus,
            pageSpecification
        ).returnValue.getOrThrow(timeout)
    }
}
