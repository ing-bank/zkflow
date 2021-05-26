package io.ivno.collateraltoken.workflow.transfer

import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.contract.TransferInitiator
import io.ivno.collateraltoken.contract.TransferSchema.TransferEntity
import io.ivno.collateraltoken.contract.TransferStatus
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
class FindTransferFlow(
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
    override val pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION
) : FindStateFlow<Transfer>() {
    override val criteria: QueryCriteria = VaultQueryCriteria(
        contractStateTypes = setOf(contractStateType),
        relevancyStatus = relevancyStatus,
        status = stateStatus
    ).andWithExpressions(
        linearId?.let { TransferEntity::linearId.equal(it.id) },
        externalId?.let { TransferEntity::externalId.equal(it) },
        currentTokenHolder?.let { TransferEntity::currentTokenHolder.equal(it) },
        targetTokenHolder?.let { TransferEntity::targetTokenHolder.equal(it) },
        initiator?.let { TransferEntity::initiator.equal(it) },
        amount?.let { TransferEntity::amount.equal(it) },
        tokenTypeLinearId?.let { TransferEntity::tokenTypeLinearId.equal(it.id) },
        tokenTypeExternalId?.let { TransferEntity::tokenTypeExternalId.equal(it) },
        transferStatus?.let { TransferEntity::status.equal(it) },
        timestamp?.let { TransferEntity::timestamp.equal(it) },
        currentTokenHolderAccountId?.let { TransferEntity::currentTokenHolderAccountId.equal(it) },
        targetTokenHolderAccountId?.let { TransferEntity::targetTokenHolderAccountId.equal(it) }
    )
}
