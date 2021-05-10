package io.ivno.collateraltoken.workflow.token

import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.IvnoTokenTypeSchema.IvnoTokenTypeEntity
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.core.workflow.DEFAULT_PAGE_SPECIFICATION
import io.onixlabs.corda.core.workflow.FindStateFlow
import io.onixlabs.corda.core.workflow.andWithExpressions
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria

@StartableByRPC
class FindIvnoTokenTypeFlow(
    linearId: UniqueIdentifier? = null,
    externalId: String? = null,
    network: Network? = null,
    networkName: String? = null,
    networkOperator: AbstractParty? = null,
    networkHash: SecureHash? = null,
    tokenIssuingEntity: AbstractParty? = null,
    custodian: AbstractParty? = null,
    displayName: String? = null,
    fractionDigits: Int? = null,
    stateStatus: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
    relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
    override val pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION
) : FindStateFlow<IvnoTokenType>() {
    override val criteria: QueryCriteria = VaultQueryCriteria(
        contractStateTypes = setOf(contractStateType),
        relevancyStatus = relevancyStatus,
        status = stateStatus
    ).andWithExpressions(
        linearId?.let { IvnoTokenTypeEntity::linearId.equal(it.id) },
        externalId?.let { IvnoTokenTypeEntity::externalId.equal(it) },
        network?.let { IvnoTokenTypeEntity::networkHash.equal(it.hash.toString()) },
        networkName?.let { IvnoTokenTypeEntity::networkName.equal(it) },
        networkOperator?.let { IvnoTokenTypeEntity::networkOperator.equal(it) },
        networkHash?.let { IvnoTokenTypeEntity::networkHash.equal(it.toString()) },
        tokenIssuingEntity?.let { IvnoTokenTypeEntity::tokenIssuingEntity.equal(it) },
        custodian?.let { IvnoTokenTypeEntity::custodian.equal(it) },
        displayName?.let { IvnoTokenTypeEntity::displayName.equal(it) },
        fractionDigits?.let { IvnoTokenTypeEntity::fractionDigits.equal(it) }
    )
}
