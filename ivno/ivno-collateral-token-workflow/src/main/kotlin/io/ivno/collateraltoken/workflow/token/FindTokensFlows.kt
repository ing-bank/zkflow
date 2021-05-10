package io.ivno.collateraltoken.workflow.token

import io.dasl.contracts.v1.token.TokenState
import io.dasl.contracts.v1.token.TokenState.TokenSchemaV1.PersistedToken
import io.ivno.collateraltoken.workflow.IVNO_DEFAULT_PAGE_SPECIFICATION
import io.onixlabs.corda.core.workflow.DEFAULT_SORTING
import io.onixlabs.corda.core.workflow.FindStatesFlow
import io.onixlabs.corda.core.workflow.andWithExpressions
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort

/**
 * Represents a query flow for finding [TokenState] states by pointer.
 *
 * @param tokenTypeSymbol The token symbol from which to obtain matching tokens.
 * @param stateStatus The state status of the states in the vault.
 * @param relevancyStatus The relevancy status of the states in the vault.
 * @param pageSpecification The page specification which determines how many results to return from the query.
 */
@StartableByRPC
class FindTokensFlowBySymbol(
    tokenTypeSymbol: String,
    stateStatus: Vault.StateStatus = Vault.StateStatus.UNCONSUMED,
    relevancyStatus: Vault.RelevancyStatus = Vault.RelevancyStatus.RELEVANT,
    override val pageSpecification: PageSpecification = IVNO_DEFAULT_PAGE_SPECIFICATION,
    override val sorting: Sort = DEFAULT_SORTING
) : FindStatesFlow<TokenState>() {
    override val criteria: QueryCriteria = QueryCriteria.VaultQueryCriteria(
        contractStateTypes = setOf(contractStateType),
        relevancyStatus = relevancyStatus,
        status = stateStatus
    ).andWithExpressions(
        tokenTypeSymbol.let { PersistedToken::tokenTypeSymbol.equal(tokenTypeSymbol) }
    )
}
