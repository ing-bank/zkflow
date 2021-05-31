package io.ivno.collateraltoken.workflow.deposit

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.DepositStatus
import io.ivno.collateraltoken.workflow.*
import io.ivno.collateraltoken.workflow.token.CreateIvnoTokenTypeFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.SignedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class RequestDepositFlowTests : FlowTest() {

    private lateinit var transaction: SignedTransaction
    private lateinit var deposit: StateAndRef<Deposit>

    override fun initialize() {

        Pipeline
            .create(network)
            .run(tieNode) {
                CreateIvnoTokenTypeFlow.Initiator(TOKEN_TYPE, null, TOKEN_TYPE_OBSERVERS)
            }
            .run(bankNodeA) {
                CreateAccountFlow(tags = setOf(Tag("default", "true")))
            }
            .run(bankNodeA) {
                val account = it.singleOutputOfType<AccountState>()
                RequestDepositFlow.Initiator(DEPOSIT.copy(accountId = account.address.accountId))
            }
            .finally {
                transaction = it
                deposit = transaction.singleOutRefOfType()
            }
    }

    @Test
    fun `RequestDepositFlow should be signed by the depositor`() {
        transaction.verifyRequiredSignatures()
    }

    @Test
    fun `RequestDepositFlow should record a transaction for the depositor, token issuing entity and the custodian`() {
        listOf(bankNodeA, tieNode, custodianNode).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                assertEquals(transaction, recordedTransaction, "Transactions are not equal.")
            }
        }
    }

    @Test
    fun `RequestDepositFlow should record a deposit state for the depositor, token issuing entity and the custodian`() {
        listOf(bankNodeA, tieNode, custodianNode).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                val recordedDeposit = recordedTransaction.tx.outputsOfType<Deposit>().singleOrNull()
                    ?: fail("Failed to find a deposit state in the recorded transaction.")

                assertEquals(recordedDeposit, deposit.state.data, "Deposits are not equal.")
                assertEquals(recordedDeposit.status, DepositStatus.DEPOSIT_REQUESTED, "Deposit statuses are not equal.")
            }
        }
    }
}