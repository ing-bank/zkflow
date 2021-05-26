package io.ivno.collateraltoken.integration

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.dasl.workflows.api.flows.account.FindAccountsFlow
import io.dasl.workflows.api.flows.account.FindAllAccountsWithPartyAndTag
import io.dasl.workflows.api.flows.account.GetAccountFlow
import io.dasl.workflows.api.flows.account.ListAllAccountsFlow
import io.onixlabs.corda.core.integration.RPCService
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.vault.DEFAULT_PAGE_NUM
import net.corda.core.node.services.vault.DEFAULT_PAGE_SIZE
import net.corda.core.utilities.getOrThrow
import java.time.Duration

class AccountQueryService(rpc: CordaRPCOps) : RPCService(rpc) {

    fun findAccountById(accountId: String, timeout: Duration = Duration.ofSeconds(30)): StateAndRef<AccountState> {
        return rpc
            .startFlow(::GetAccountFlow, accountId)
            .returnValue
            .getOrThrow(timeout)
    }

    fun findAllAccounts(
        pageNumber: Int = DEFAULT_PAGE_NUM,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        timeout: Duration = Duration.ofSeconds(30)
    ): Set<StateAndRef<AccountState>> {
        return rpc
            .startFlow(::ListAllAccountsFlow, pageNumber, pageSize, null)
            .returnValue
            .getOrThrow(timeout)
    }

    fun findAccountsByTag(
        tag: Tag,
        pageNumber: Int = DEFAULT_PAGE_NUM,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        timeout: Duration = Duration.ofSeconds(30)
    ): Set<AccountState> {
        return rpc
            .startFlow(::FindAccountsFlow, tag, pageNumber, pageSize)
            .returnValue
            .getOrThrow(timeout)
    }

    fun findAccountsByOwner(
        owner: Party,
        pageNumber: Int = DEFAULT_PAGE_NUM,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        timeout: Duration = Duration.ofSeconds(30)
    ): Set<StateAndRef<AccountState>> {
        return rpc
            .startFlow(::ListAllAccountsFlow, pageNumber, pageSize, owner)
            .returnValue
            .getOrThrow(timeout)
    }

    fun findDefaultAccountByOwner(
        owner: Party,
        pageNumber: Int = DEFAULT_PAGE_NUM,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        timeout: Duration = Duration.ofSeconds(30)
    ): Set<StateAndRef<AccountState>> {
        return rpc
            .startFlow(::FindAllAccountsWithPartyAndTag, pageNumber, pageSize, owner, Tag("default", "true"))
            .returnValue
            .getOrThrow(timeout)
    }
}
