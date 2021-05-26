package io.ivno.collateraltoken.integration

import io.dasl.contracts.v1.token.TokenTypeState
import io.ivno.collateraltoken.workflow.GetTokenBalanceFlow
import io.ivno.collateraltoken.workflow.GetTokenBalanceForAccountsFlow
import io.onixlabs.corda.core.integration.RPCService
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import java.math.BigDecimal
import java.time.Duration

class TokenQueryService(rpc: CordaRPCOps) : RPCService(rpc) {

    fun getTokenBalances(
        timeout: Duration = Duration.ofSeconds(30)
    ): Map<TokenTypeState, BigDecimal> {
        return rpc
            .startFlow(::GetTokenBalanceFlow)
            .returnValue
            .getOrThrow(timeout)
    }

    fun getAccountTokenBalances(
        accountId: String? = null,
        timeout: Duration = Duration.ofSeconds(30)
    ): Map<String, Map<TokenTypeState, BigDecimal>> {
        return rpc
            .startFlow(::GetTokenBalanceForAccountsFlow, accountId)
            .returnValue
            .getOrThrow(timeout)
    }
}
