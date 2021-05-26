package io.ivno.collateraltoken.workflow.redemption

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.dasl.contracts.v1.token.TokenState
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
import io.ivno.collateraltoken.workflow.transfer.AcceptTransferFlow
import io.ivno.collateraltoken.workflow.transfer.RequestTransferFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.SignedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class RejectRedemptionFlowTests : FlowTest() {

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
            .run(bankNodeA) {
                val requestedTransfer = TRANSFER_SEND.copy(
                    currentTokenHolderAccountId = accountIdBankA,
                    targetTokenHolderAccountId = accountIdBankB
                )
                RequestTransferFlow.Initiator(requestedTransfer)
            }
            .run(bankNodeB) {
                val requestedTransfer = it.singleOutRefOfType<Transfer>()
                val acceptedTransfer = requestedTransfer.state.data.acceptTransfer()
                AcceptTransferFlow.Initiator(requestedTransfer, acceptedTransfer)
            }
            .run(bankNodeB) {
                val requestedRedemption = REDEMPTION.copy(accountId = accountIdBankB)
                RequestRedemptionFlow.Initiator(requestedRedemption)
            }
            .run(custodianNode) {
                val requestedRedemption = it.singleOutRefOfType<Redemption>()
                val rejectedRedemption = requestedRedemption.state.data.rejectRedemption()
                RejectRedemptionFlow.Initiator(requestedRedemption, rejectedRedemption)
            }
            .finally {
                transaction = it
                redemption = transaction.singleOutRefOfType()
            }
    }

    @Test
    fun `RejectRedemptionFlow should be signed by the custodian`() {
        transaction.verifySignaturesExcept(REDEMPTION.tokenIssuingEntity.owningKey)
    }

    @Test
    fun `RejectRedemptionFlow should be signed by the token issuing entity`() {
        transaction.verifySignaturesExcept(REDEMPTION.custodian.owningKey)
    }

    @Test
    fun `RejectRedemptionFlow should record a transaction for the redeemer, custodian and token issuing entity`() {
        listOf(bankNodeB, custodianNode, tieNode).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                assertEquals(transaction, recordedTransaction, "Transactions are not equal.")
            }
        }
    }

    @Test
    fun `RejectRedemptionFlow should record a transfer state for the redeemer, custodian and token issuing entity`() {
        listOf(bankNodeB, custodianNode, tieNode).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                val recordedRedemption = recordedTransaction.tx.outputsOfType<Redemption>().singleOrNull()
                    ?: fail("Failed to find a redemption state in the recorded transaction.")

                val recordedToken = recordedTransaction.tx.outputsOfType<TokenState>().singleOrNull()
                    ?: fail("Failed to find a token state in the recorded transaction.")

                assertEquals(recordedRedemption, redemption.state.data, "Redemption state is not equal.")
                assertEquals(recordedRedemption.status, RedemptionStatus.REJECTED, "Redemption status is not equal.")
                assertEquals(recordedRedemption.redeemer, recordedToken.owner)
                assert(recordedRedemption.amount.quantity.compareTo(recordedToken.amount.quantity) == 0)
            }
        }
    }
}
