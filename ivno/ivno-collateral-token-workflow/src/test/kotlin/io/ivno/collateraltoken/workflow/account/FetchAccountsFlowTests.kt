package io.ivno.collateraltoken.workflow.account

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.ivno.collateraltoken.workflow.CreateAccountFlow
import io.ivno.collateraltoken.workflow.FetchAccountsFlow
import io.ivno.collateraltoken.workflow.FlowTest
import io.ivno.collateraltoken.workflow.Pipeline
import net.corda.core.transactions.SignedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class FetchAccountsFlowTests : FlowTest() {
    private lateinit var transaction: SignedTransaction

    override fun initialize() {
        Pipeline
            .create(network)
            .run(bankNodeA) {
                CreateAccountFlow(tags = setOf(Tag("name", "Party A Account 1")))
            }
            .run(bankNodeB) {
                FetchAccountsFlow.Initiator(setOf(bankPartyA))
            }
            .run(bankNodeA) {
                CreateAccountFlow(tags = setOf(Tag("name", "Party A Account 2")))
            }
            .run(bankNodeB) {
                transaction = it
                FetchAccountsFlow.Initiator(setOf(tieParty, bankPartyA))
            }
    }

    @Test
    fun `FetchAccountsFlow should record a transaction for the initiating party`() {
        val recordedTransaction = bankNodeB.services.validatedTransactions.getTransaction(transaction.id)
            ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")
        assertEquals(recordedTransaction, transaction, "Transactions are not equal.")
    }

    @Test
    fun `FetchAccountsFlow should record an account state for the initiating party`() {
        val recordedStates = bankNodeB.services.vaultService.queryBy(AccountState::class.java).states
        assertEquals(2, recordedStates.size)
    }
}
