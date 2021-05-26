package io.ivno.collateraltoken.workflow

import co.paralleluniverse.fibers.Suspendable
import io.dasl.contracts.v1.tag.Tag
import io.dasl.workflows.api.flows.account.CreateAccountFlow
import io.dasl.workflows.api.flows.account.CreateAccountFlow.Request
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import org.apache.commons.lang3.RandomStringUtils

@StartableByRPC
class CreateAccountFlow(
    private val accountId: String = RandomStringUtils.randomNumeric(8),
    private val tags: Set<Tag> = emptySet(),
    private val notary: Party? = null
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        checkHasNoAccountWithTheSameName(tags)
        return subFlow(
            CreateAccountFlow(
                requests = listOf(Request(accountId, tags.toList())),
                notary = notary ?: getPreferredNotary()
            )
        )
    }
}
