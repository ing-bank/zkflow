package io.ivno.collateraltoken.integration

import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.DepositStatus
import io.ivno.collateraltoken.workflow.deposit.FindDepositFlow
import io.ivno.collateraltoken.workflow.deposit.FindDepositsFlow
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

class DepositQueryService(rpc: CordaRPCOps) : RPCService(rpc) {

    fun findDeposits(
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
        pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION,
        sorting: Sort = DEFAULT_SORTING,
        timeout: Duration = Duration.ofSeconds(30)
    ): List<StateAndRef<Deposit>> {
        return rpc.startFlowDynamic(
            FindDepositsFlow::class.java,
            linearId,
            externalId,
            depositor,
            custodian,
            tokenIssuingEntity,
            amount,
            tokenTypeLinearId,
            tokenTypeExternalId,
            reference,
            depositStatus,
            timestamp,
            accountId,
            stateStatus,
            relevancyStatus,
            pageSpecification,
            sorting
        ).returnValue.getOrThrow(timeout)
    }

    fun findDeposit(
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
        pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION,
        timeout: Duration = Duration.ofSeconds(30)
    ): StateAndRef<Deposit>? {
        return rpc.startFlowDynamic(
            FindDepositFlow::class.java,
            linearId,
            externalId,
            depositor,
            custodian,
            tokenIssuingEntity,
            amount,
            tokenTypeLinearId,
            tokenTypeExternalId,
            reference,
            depositStatus,
            timestamp,
            accountId,
            stateStatus,
            relevancyStatus,
            pageSpecification
        ).returnValue.getOrThrow(timeout)
    }
}
