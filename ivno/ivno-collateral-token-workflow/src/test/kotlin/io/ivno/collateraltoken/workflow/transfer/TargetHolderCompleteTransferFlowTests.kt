package io.ivno.collateraltoken.workflow.transfer

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.dasl.contracts.v1.token.TokenState
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.contract.TransferStatus
import io.ivno.collateraltoken.workflow.*
import io.ivno.collateraltoken.workflow.deposit.AcceptDepositFlow
import io.ivno.collateraltoken.workflow.deposit.AcceptDepositPaymentFlow
import io.ivno.collateraltoken.workflow.deposit.IssueDepositPaymentFlow
import io.ivno.collateraltoken.workflow.deposit.RequestDepositFlow
import io.ivno.collateraltoken.workflow.token.CreateIvnoTokenTypeFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.SignedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class TargetHolderCompleteTransferFlowTests : FlowTest() {

    private lateinit var transaction: SignedTransaction
    private lateinit var transfer: StateAndRef<Transfer>
    private lateinit var accountIdBankA: String
    private lateinit var accountIdBankB: String

    override fun initialize() {

        Pipeline
            .create(network)
            .run(tieNode) {
                CreateIvnoTokenTypeFlow.Initiator(TOKEN_TYPE, null, TOKEN_TYPE_OBSERVERS)
            }
            .run(bankNodeA) {
                CreateAccountFlow(tags = setOf(Tag("default", "true")))
            }
            .run(bankNodeB) {
                accountIdBankA = it.singleOutputOfType<AccountState>().address.accountId
                CreateAccountFlow(tags = setOf(Tag("default", "true")))
            }
            .run(custodianNode) {
                accountIdBankB = it.singleOutputOfType<AccountState>().address.accountId
                FetchAccountsFlow.Initiator(setOf(bankPartyA, bankPartyB))
            }
            .run(bankNodeA) {
                FetchAccountsFlow.Initiator(setOf(bankPartyB))
            }
            .run(bankNodeA) {
                RequestDepositFlow.Initiator(DEPOSIT.copy(accountId = accountIdBankA))
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
                val paymentAcceptedDeposit = paymentIssuedDeposit.state.data.acceptPayment()
                AcceptDepositPaymentFlow.Initiator(paymentIssuedDeposit, paymentAcceptedDeposit)
            }
            .run(bankNodeB) {
                val requestedTransfer = TRANSFER_REQUEST.copy(
                    currentTokenHolderAccountId = accountIdBankA,
                    targetTokenHolderAccountId = accountIdBankB
                )
                RequestTransferFlow.Initiator(requestedTransfer)
            }
            .run(bankNodeA) {
                val requestedTransfer = it.singleOutRefOfType<Transfer>()
                val completedTransfer = requestedTransfer.state.data.completeTransfer()
                CompleteTransferFlow.Initiator(requestedTransfer, completedTransfer)
            }
            .finally {
                transaction = it
                transfer = transaction.singleOutRefOfType()
            }
    }

    @Test
    fun `CompleteTransferFlow should be signed by the transferee`() {
        transaction.verifyRequiredSignatures()
    }

    @Test
    fun `CompleteTransferFlow should record a transaction for the transferer and the transferee`() {
        listOf(bankNodeA, bankNodeB).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                assertEquals(transaction, recordedTransaction, "Transactions are not equal.")
            }
        }
    }

    @Test
    fun `CompleteTransferFlow should record a transfer state for the transferer and the transferee`() {
        listOf(bankNodeA, bankNodeB).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                val recordedTransfer = recordedTransaction.tx.outputsOfType<Transfer>().singleOrNull()
                    ?: fail("Failed to find a transfer state in the recorded transaction.")

                assertEquals(recordedTransfer, transfer.state.data, "Transfers are not equal.")
                assertEquals(recordedTransfer.status, TransferStatus.COMPLETED, "Transfer statuses are not equal.")
            }
        }
    }

    @Test
    fun `CompleteTransferFlow should record a fungible token states for the transferer and the transferee`() {
        listOf(bankNodeA, bankNodeB).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                val recordedTransfer = recordedTransaction.tx.outputsOfType<Transfer>().singleOrNull()
                    ?: fail("Failed to find a transfer state in the recorded transaction.")

                val recordedTokenForBankA = recordedTransaction.tx
                    .outputsOfType<TokenState>().singleOrNull { token -> token.owner == bankPartyA }
                    ?: fail("Failed to find a fungible token state for Bank A in the recorded transaction.")

                val recordedTokenForBankB = recordedTransaction.tx
                    .outputsOfType<TokenState>().singleOrNull { token -> token.owner == bankPartyB }
                    ?: fail("Failed to find a fungible token state for Bank B in the recorded transaction.")

                assertEquals(recordedTransfer, transfer.state.data, "Transfers are not equal.")
                assertEquals(recordedTransfer.status, TransferStatus.COMPLETED, "Transfer statuses are not equal.")

                assertEquals(recordedTokenForBankA.amount.quantity, 50.toBigDecimal().setScale(2))
                assertEquals(recordedTokenForBankB.amount.quantity, 50.toBigDecimal().setScale(2))
            }
        }
    }
}
