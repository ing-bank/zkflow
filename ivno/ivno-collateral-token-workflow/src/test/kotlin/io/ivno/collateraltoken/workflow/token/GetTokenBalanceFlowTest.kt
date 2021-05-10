package io.ivno.collateraltoken.workflow.token

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenTypeState
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.workflow.*
import io.ivno.collateraltoken.workflow.deposit.AcceptDepositFlow
import io.ivno.collateraltoken.workflow.deposit.AcceptDepositPaymentFlow
import io.ivno.collateraltoken.workflow.deposit.IssueDepositPaymentFlow
import io.ivno.collateraltoken.workflow.deposit.RequestDepositFlow
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class GetTokenBalanceFlowTest : FlowTest() {

    private lateinit var accountId: String

    override fun initialize() {
        val amount1 = BigDecimalAmount(BigDecimal.valueOf(123.45), TOKEN_TYPE.toPointer())
        val amount2 = BigDecimalAmount(BigDecimal.valueOf(678.90), TOKEN_TYPE.toPointer())

        Pipeline
            .create(network)
            .run(tieNode) {
                CreateIvnoTokenTypeFlow.Initiator(TOKEN_TYPE, null, TOKEN_TYPE_OBSERVERS)
            }
            .run(bankNodeA) {
                CreateAccountFlow(tags = setOf(Tag("default", "true")))
            }
            .run(custodianNode) {
                accountId = it.singleOutRefOfType<AccountState>().state.data.address.accountId
                FetchAccountsFlow.Initiator(setOf(bankPartyA))
            }
            .run(bankNodeA) {
                RequestDepositFlow.Initiator(DEPOSIT.copy(amount = amount1, accountId = accountId))
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
            }.run(bankNodeA) {
                RequestDepositFlow.Initiator(DEPOSIT.copy(amount = amount2, accountId = accountId))
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
    }

    @Test
    fun `GetTokenBalanceFlow should return the correct token balance`() {
        val expected: Map<TokenTypeState, BigDecimal> = mapOf(
            TOKEN_TYPE.toPointer().resolve(bankNodeA.services).state.data to BigDecimal.valueOf(802.35)
        )

        val actual: Map<TokenTypeState, BigDecimal> = Pipeline
            .create(network)
            .run(bankNodeA) { GetTokenBalanceFlow() }
            .result

        assertEquals(expected, actual)
    }
}
