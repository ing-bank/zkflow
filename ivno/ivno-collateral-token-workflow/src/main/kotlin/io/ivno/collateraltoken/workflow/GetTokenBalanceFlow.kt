package io.ivno.collateraltoken.workflow

import co.paralleluniverse.fibers.Suspendable
import io.dasl.contracts.v1.token.TokenState
import io.dasl.contracts.v1.token.TokenTypeState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import java.math.BigDecimal

/**
 * Represents a flow which encapsulates vault query logic for finding token type balances.
 *
 * @property criteria The criteria for finding token type balances.
 */
@StartableByRPC
class GetTokenBalanceFlow(
    private val criteria: QueryCriteria
) : FlowLogic<Map<TokenTypeState, BigDecimal>>() {

    /**
     * Finds balances for all token types.
     */
    constructor() : this(builder {
        QueryCriteria.VaultQueryCriteria(
            contractStateTypes = setOf(TokenState::class.java)
        )
    })

    @Suspendable
    override fun call(): Map<TokenTypeState, BigDecimal> {

        return serviceHub
            .vaultService
            .queryBy<TokenState>(criteria)
            .states
            .map { it.state.data }
            .groupBy { it.tokenTypePointer }
            .map { it.key.resolve(serviceHub).state.data to it.value.sum() }
            .toMap()
    }

    private fun List<TokenState>.sum(): BigDecimal {
        return map { it.amount.quantity }.fold(BigDecimal.ZERO, BigDecimal::add)
    }
}
