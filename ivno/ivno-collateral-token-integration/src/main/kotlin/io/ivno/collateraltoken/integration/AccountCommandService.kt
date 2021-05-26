package io.ivno.collateraltoken.integration

import io.dasl.contracts.v1.tag.Tag
import io.ivno.collateraltoken.workflow.CreateAccountFlow
import io.ivno.collateraltoken.workflow.FetchAccountsFlow
import io.onixlabs.corda.core.integration.RPCService
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowProgressHandle
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor
import org.apache.commons.lang3.RandomStringUtils

class AccountCommandService(rpc: CordaRPCOps) : RPCService(rpc) {

    private companion object {
        val logger = loggerFor<AccountCommandService>()
    }

    fun createAccount(
        accountId: String = RandomStringUtils.randomNumeric(8),
        tags: Set<Tag> = emptySet(),
        notary: Party? = null
    ): FlowProgressHandle<SignedTransaction> {
        logger.info("Creating account: $accountId")
        return rpc.startTrackedFlow(::CreateAccountFlow, accountId, tags, notary)
    }

    fun fetchAccounts(counterparties: Set<Party>): FlowProgressHandle<Unit> {
        logger.info("Fetching accounts.")
        return rpc.startTrackedFlow(FetchAccountsFlow::Initiator, counterparties)
    }
}
