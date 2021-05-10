package io.ivno.collateraltoken.workflow.redemption

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.workflow.*
import io.ivno.collateraltoken.workflow.deposit.AcceptDepositFlow
import io.ivno.collateraltoken.workflow.deposit.AcceptDepositPaymentFlow
import io.ivno.collateraltoken.workflow.deposit.IssueDepositPaymentFlow
import io.ivno.collateraltoken.workflow.deposit.RequestDepositFlow
import io.ivno.collateraltoken.workflow.token.CreateIvnoTokenTypeFlow
import io.ivno.collateraltoken.workflow.transfer.AcceptTransferFlow
import io.ivno.collateraltoken.workflow.transfer.RequestTransferFlow
import net.corda.core.contracts.UniqueIdentifier
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FindRedemptionsFlowTests : FlowTest() {

    private val externalId = "Redemption001"
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
                val requestedRedemption = REDEMPTION.copy(
                    linearId = UniqueIdentifier(externalId),
                    accountId = accountIdBankB
                )
                RequestRedemptionFlow.Initiator(requestedRedemption)
            }
            .run(bankNodeB) {
                val requestedRedemption = REDEMPTION.copy(
                    linearId = UniqueIdentifier(externalId),
                    accountId = accountIdBankB,
                    amount = TOKEN_OF_10GBP
                )
                RequestRedemptionFlow.Initiator(requestedRedemption)
            }
    }

    @Test
    fun `FindRedemptionsFlow should return the correct number of redemption states when specifying the externalId`() {
        Pipeline
            .create(network)
            .run(bankNodeB) { FindRedemptionsFlow(externalId = externalId) }
            .finally { assertEquals(2, it.size) }
    }
}
