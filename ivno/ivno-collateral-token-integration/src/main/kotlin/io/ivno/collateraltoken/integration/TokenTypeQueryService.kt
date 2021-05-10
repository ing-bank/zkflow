package io.ivno.collateraltoken.integration

import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.workflow.token.FindIvnoTokenTypeFlow
import io.ivno.collateraltoken.workflow.token.FindIvnoTokenTypesFlow
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.core.integration.RPCService
import io.onixlabs.corda.core.workflow.DEFAULT_PAGE_SPECIFICATION
import io.onixlabs.corda.core.workflow.DEFAULT_SORTING
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.Sort
import net.corda.core.utilities.getOrThrow
import java.time.Duration

class TokenTypeQueryService(rpc: CordaRPCOps) : RPCService(rpc) {

    fun findIvnoTokenTypes(
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
        stateStatus: Vault.StateStatus = Vault.StateStatus.ALL,
        relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
        pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION,
        sorting: Sort = DEFAULT_SORTING,
        timeout: Duration = Duration.ofSeconds(30)
    ): List<StateAndRef<IvnoTokenType>> {
        return rpc.startFlowDynamic(
            FindIvnoTokenTypesFlow::class.java,
            linearId,
            externalId,
            network,
            networkName,
            networkOperator,
            networkHash,
            tokenIssuingEntity,
            custodian,
            displayName,
            fractionDigits,
            stateStatus,
            relevancyStatus,
            pageSpecification,
            sorting
        ).returnValue.getOrThrow(timeout)
    }

    fun findIvnoTokenType(
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
        stateStatus: Vault.StateStatus = Vault.StateStatus.ALL,
        relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.ALL,
        pageSpecification: PageSpecification = DEFAULT_PAGE_SPECIFICATION,
        timeout: Duration = Duration.ofSeconds(30)
    ): StateAndRef<IvnoTokenType>? {
        return rpc.startFlowDynamic(
            FindIvnoTokenTypeFlow::class.java,
            linearId,
            externalId,
            network,
            networkName,
            networkOperator,
            networkHash,
            tokenIssuingEntity,
            custodian,
            displayName,
            fractionDigits,
            stateStatus,
            relevancyStatus,
            pageSpecification
        ).returnValue.getOrThrow(timeout)
    }
}
