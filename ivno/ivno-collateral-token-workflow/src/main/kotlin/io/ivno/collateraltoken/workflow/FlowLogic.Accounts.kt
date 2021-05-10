package io.ivno.collateraltoken.workflow

import co.paralleluniverse.fibers.Suspendable
import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.dasl.workflows.api.flows.account.FindAccountsFlow
import io.dasl.workflows.api.flows.account.GetAccountFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap

/**
 * Checks that an account with the same name does not exist.
 *
 * @param tags The account tags from which to obtain a name tag.
 * @throws FlowException if an account with the specified name already exists.
 */
@Suspendable
fun FlowLogic<*>.checkHasNoAccountWithTheSameName(tags: Iterable<Tag>) {
    tags.singleOrNull { it.category == "name" }?.let {
        if (subFlow(FindAccountsFlow(it, IVNO_DEFAULT_PAGE_NUMBER, IVNO_DEFAULT_PAGE_SIZE)).isNotEmpty()) {
            throw FlowException("Account with the specified name already exists: ${it.value}.")
        }
    }
}

@Suspendable
fun FlowLogic<*>.resolveAccount(
    accountId: String,
    party: Party = ourIdentity,
    sessions: Iterable<FlowSession> = emptySet()
): StateAndRef<AccountState> {
    return if (party in serviceHub.myInfo.legalIdentities) {
        try {
            subFlow(GetAccountFlow(accountId))
        } catch (ex: IllegalArgumentException) {
            throw FlowException("Account with the specified account ID does not exist: $accountId.")
        }
    } else {
        sessions.forEach { it.send(it.counterparty == party) }
        val partySession = sessions.single { it.counterparty == party }
        return try {
            val account = subFlow(GetAccountFlow(accountId))
            partySession.send(false)
            account
        } catch (ex: IllegalArgumentException) {
            partySession.send(true)
            subFlow(FetchAccountFlow(partySession, accountId))
            subFlow(GetAccountFlow(accountId))
        }
    }
}

@Suspendable
fun FlowLogic<*>.resolveAccountHandler(session: FlowSession) {
    val isAskedToResolve = session.receive<Boolean>().unwrap { it }
    if (isAskedToResolve) {
        val isAskedForAccount = session.receive<Boolean>().unwrap { it }
        if (isAskedForAccount) {
            subFlow(FetchAccountFlow.Observer(session))
        }
    }
}
