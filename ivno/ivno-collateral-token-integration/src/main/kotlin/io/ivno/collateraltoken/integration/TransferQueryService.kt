package io.ivno.collateraltoken.integration

import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.contract.TransferInitiator
import io.ivno.collateraltoken.contract.TransferStatus
import io.ivno.collateraltoken.workflow.transfer.FindTransferFlow
import io.ivno.collateraltoken.workflow.transfer.FindTransfersFlow
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

class TransferQueryService(rpc: CordaRPCOps) : RPCService(rpc) {

    fun findTransfer(
        linearId: UniqueIdentifier? = null,
        externalId: String? = null,
        currentTokenHolder: AbstractParty? = null,
        targetTokenHolder: AbstractParty? = null,
        initiator: TransferInitiator? = null,
        amount: BigDecimal? = null,
        tokenTypeLinearId: UniqueIdentifier? = null,
        tokenTypeExternalId: String? = null,
        transferStatus: TransferStatus? = null,
        timestamp: Instant? = null,
        currentTokenHolderAccountId: String? = null,
        targetTokenHolderAccountId: String? = null,
        stateStatus: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
        relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
        pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION,
        timeout: Duration = Duration.ofSeconds(30)
    ): StateAndRef<Transfer>? {
        return rpc.startFlowDynamic(
            FindTransferFlow::class.java,
            linearId,
            externalId,
            currentTokenHolder,
            targetTokenHolder,
            initiator,
            amount,
            tokenTypeLinearId,
            tokenTypeExternalId,
            transferStatus,
            timestamp,
            currentTokenHolderAccountId,
            targetTokenHolderAccountId,
            stateStatus,
            relevancyStatus,
            pageSpecification
        ).returnValue.getOrThrow(timeout)
    }

    fun findTransfers(
        linearId: UniqueIdentifier? = null,
        externalId: String? = null,
        currentTokenHolder: AbstractParty? = null,
        targetTokenHolder: AbstractParty? = null,
        initiator: TransferInitiator? = null,
        amount: BigDecimal? = null,
        tokenTypeLinearId: UniqueIdentifier? = null,
        tokenTypeExternalId: String? = null,
        transferStatus: TransferStatus? = null,
        timestamp: Instant? = null,
        currentTokenHolderAccountId: String? = null,
        targetTokenHolderAccountId: String? = null,
        stateStatus: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
        relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
        pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION,
        sorting: Sort = DEFAULT_SORTING,
        timeout: Duration = Duration.ofSeconds(30)
    ): List<StateAndRef<Transfer>> {
        return rpc.startFlowDynamic(
            FindTransfersFlow::class.java,
            linearId,
            externalId,
            currentTokenHolder,
            targetTokenHolder,
            initiator,
            amount,
            tokenTypeLinearId,
            tokenTypeExternalId,
            transferStatus,
            timestamp,
            currentTokenHolderAccountId,
            targetTokenHolderAccountId,
            stateStatus,
            relevancyStatus,
            pageSpecification,
            sorting
        ).returnValue.getOrThrow(timeout)
    }
}
