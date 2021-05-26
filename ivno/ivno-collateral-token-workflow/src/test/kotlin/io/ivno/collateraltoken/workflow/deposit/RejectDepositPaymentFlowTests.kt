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
import kotlin.test.assertEquals
import kotlin.test.fail

class RejectDepositPaymentFlowTests : FlowTest() {

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
            .run(custodianNode) {
                val requestedDeposit = it.singleOutRefOfType<Deposit>()
                val acceptedDeposit = requestedDeposit.state.data.acceptDeposit()
                AcceptDepositFlow.Initiator(requestedDeposit, acceptedDeposit)
            }
            .run(bankNodeA) {
                val acceptedDeposit = it.singleOutRefOfType<Deposit>()
                val paymentIssuedDeposit = acceptedDeposit.state.data.issuePayment()
                IssueDepositPaymentFlow.Initiator(acceptedDeposit, paymentIssuedDeposit)
            }
            .run(custodianNode) {
                val paymentIssuedDeposit = it.singleOutRefOfType<Deposit>()
                val paymentRejectedDeposit = paymentIssuedDeposit.state.data.rejectPayment()
                RejectDepositPaymentFlow.Initiator(paymentIssuedDeposit, paymentRejectedDeposit)
            }
            .finally {
                transaction = it
                deposit = transaction.singleOutRefOfType()
            }
    }

    @Test
    fun `AdvanceDepositFlow should be signed by the custodian`() {
        transaction.verifyRequiredSignatures()
    }

    @Test
    fun `AdvanceDepositFlow should record a transaction for the depositor, token issuing entity and the custodian`() {
        listOf(bankNodeA, tieNode, custodianNode).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                assertEquals(transaction, recordedTransaction, "Transactions are not equal.")
            }
        }
    }

    @Test
    fun `AdvanceDepositFlow should record a deposit state for the depositor, token issuing entity and the custodian`() {
        listOf(bankNodeA, tieNode, custodianNode).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                val recordedDeposit = recordedTransaction.tx.outputsOfType<Deposit>().singleOrNull()
                    ?: fail("Failed to find a deposit state in the recorded transaction.")

                assertEquals(recordedDeposit, deposit.state.data, "Deposits are not equal.")
                assertEquals(recordedDeposit.status, DepositStatus.PAYMENT_REJECTED, "Deposit statuses are not equal.")
            }
        }
    }
}
