package io.ivno.collateraltoken.workflow.redemption

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.contract.RedemptionStatus
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.workflow.*
import io.ivno.collateraltoken.workflow.deposit.AcceptDepositFlow
import io.ivno.collateraltoken.workflow.deposit.AcceptDepositPaymentFlow
import io.ivno.collateraltoken.workflow.deposit.IssueDepositPaymentFlow
import io.ivno.collateraltoken.workflow.deposit.RequestDepositFlow
import io.ivno.collateraltoken.workflow.token.CreateIvnoTokenTypeFlow
import io.ivno.collateraltoken.workflow.token.FindTokensFlowBySymbol
import io.ivno.collateraltoken.workflow.transfer.CompleteTransferFlow
import io.ivno.collateraltoken.workflow.transfer.RequestTransferFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.SignedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import kotlin.test.assertEquals

class CompleteRedemptionFlowTests : FlowTest() {

    private lateinit var transaction: SignedTransaction
    private lateinit var redemption: StateAndRef<Redemption>
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
            .run(bankNodeB) {
                val requestedRedemption = REDEMPTION.copy(accountId = accountIdBankB)
                RequestRedemptionFlow.Initiator(requestedRedemption)
            }
            .run(custodianNode) {
                val requestedRedemption = it.singleOutRefOfType<Redemption>()
                val completedRedemption = requestedRedemption.state.data.completeRedemption()
                CompleteRedemptionFlow.Initiator(requestedRedemption, completedRedemption)
            }
            .finally {
                transaction = it
                redemption = transaction.singleOutRefOfType()
            }
    }

    @Test
    fun `CompleteRedemptionFlow should be signed by the custodian`() {
        transaction.verifyRequiredSignatures()
    }

    @Test
    fun `CompleteRedemptionFlow should record a transaction for the redeemer, custodian and token issuing entity`() {
        listOf(bankNodeB, custodianNode, tieNode).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                assertEquals(transaction, recordedTransaction, "Transactions are not equal.")
            }
        }
    }

    @Test
    fun `CompleteRedemptionFlow should record a transfer state for the redeemer, custodian and token issuing entity`() {
        listOf(bankNodeB, custodianNode, tieNode).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                val recordedRedemption = recordedTransaction.tx.outputsOfType<Redemption>().singleOrNull()
                    ?: fail("Failed to find a redemption state in the recorded transaction.")

                assertEquals(recordedRedemption, redemption.state.data, "Redemptions are not equal.")
                assertEquals(recordedRedemption.status, RedemptionStatus.COMPLETED, "Redemption status is not equal.")
            }
        }
    }

    @Test
    fun `CompleteRedemptionFlow should result in the correct number of remaining tokens held by the redeemer`() {
        val tokenBalance = Pipeline
            .create(network)
            .run(bankNodeB) {
                FindTokensFlowBySymbol(
                    TOKEN_TYPE.symbol
                )
            }
            .result
            .map { it.state.data.amount.quantity }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        assertEquals(20.toBigDecimal().setScale(2), tokenBalance)
    }
}
