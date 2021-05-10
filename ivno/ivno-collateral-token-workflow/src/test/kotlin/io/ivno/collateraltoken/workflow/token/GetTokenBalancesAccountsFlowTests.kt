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

class GetTokenBalancesAccountsFlowTests : FlowTest() {

    private lateinit var defaultAccountId: String
    private lateinit var accountId: String

    override fun initialize() {
        val amount1 = BigDecimalAmount(BigDecimal.valueOf(123.45), TOKEN_TYPE.toPointer())
        val amount2 = BigDecimalAmount(BigDecimal.valueOf(678.90), TOKEN_TYPE.toPointer())
        val amount3 = BigDecimalAmount(BigDecimal.valueOf(300.00), TOKEN_TYPE.toPointer())

        Pipeline
            .create(network)
            .run(tieNode) {
                CreateIvnoTokenTypeFlow.Initiator(TOKEN_TYPE, null, TOKEN_TYPE_OBSERVERS)
            }
            .run(bankNodeA) {
                CreateAccountFlow(tags = setOf(Tag("name", "Account 1")))
            }
            .run(bankNodeA) {
                accountId = it.singleOutRefOfType<AccountState>().state.data.address.accountId
                CreateAccountFlow(tags = setOf(Tag("name", "Default Account"), Tag("default", "true")))
            }
            .run(custodianNode) {
                defaultAccountId = it.singleOutRefOfType<AccountState>().state.data.address.accountId
                FetchAccountsFlow.Initiator(setOf(bankPartyA))
            }
            .run(bankNodeA) {
                RequestDepositFlow.Initiator(DEPOSIT.copy(amount = amount1, accountId = defaultAccountId))
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
                RequestDepositFlow.Initiator(DEPOSIT.copy(amount = amount2, accountId = defaultAccountId))
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
                RequestDepositFlow.Initiator(DEPOSIT.copy(amount = amount3, accountId = accountId))
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
    fun `GetTokenBalanceForAccountsFlow should return the correct token balances`() {
        val defaultAccountBalance: Map<TokenTypeState, BigDecimal> =
            mapOf(TOKEN_TYPE.toPointer().resolve(bankNodeA.services).state.data to BigDecimal.valueOf(802.35))
        val accountBalance: Map<TokenTypeState, BigDecimal> =
            mapOf(
                TOKEN_TYPE.toPointer().resolve(bankNodeA.services).state.data to BigDecimal.valueOf(300.00).setScale(
                    TOKEN_TYPE.fractionDigits
                )
            )

        val expected: Map<String, Map<TokenTypeState, BigDecimal>> = mapOf(
            defaultAccountId to defaultAccountBalance,
            accountId to accountBalance
        )

        val actual: Map<String, Map<TokenTypeState, BigDecimal>> = Pipeline
            .create(network)
            .run(bankNodeA) { GetTokenBalanceForAccountsFlow() }
            .result

        assertEquals(expected, actual)
    }

    @Test
    fun `GetTokenBalanceForAccountsFlow should return the correct token balances filtering by account id`() {
        val defaultAccountBalance: Map<TokenTypeState, BigDecimal> = mapOf(
            TOKEN_TYPE.toPointer().resolve(bankNodeA.services).state.data to BigDecimal.valueOf(802.35)
        )

        val expected: Map<String, Map<TokenTypeState, BigDecimal>> = mapOf(
            defaultAccountId to defaultAccountBalance
        )

        val actual: Map<String, Map<TokenTypeState, BigDecimal>> = Pipeline
            .create(network)
            .run(bankNodeA) { GetTokenBalanceForAccountsFlow(defaultAccountId) }
            .result

        assertEquals(expected, actual)
    }
}
