package io.ivno.collateraltoken.workflow.deposit

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.tag.Tag
import io.ivno.collateraltoken.workflow.CreateAccountFlow
import io.ivno.collateraltoken.workflow.FlowTest
import io.ivno.collateraltoken.workflow.Pipeline
import io.ivno.collateraltoken.workflow.singleOutRefOfType
import io.ivno.collateraltoken.workflow.token.CreateIvnoTokenTypeFlow
import net.corda.core.contracts.UniqueIdentifier
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FindDepositsFlowTests : FlowTest() {

    private lateinit var accountId: String
    private val id = UniqueIdentifier(externalId = "Deposit001")

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
                accountId = it.singleOutRefOfType<AccountState>().state.data.address.accountId
                RequestDepositFlow.Initiator(DEPOSIT.copy(linearId = id, accountId = accountId))
            }
            .run(bankNodeA) {
                RequestDepositFlow.Initiator(DEPOSIT.copy(linearId = id, accountId = accountId))
            }
    }

    @Test
    fun `FindDepositsFlow should return the correct number of deposit states when specifying the externalId`() {
        val deposits = Pipeline
            .create(network)
            .run(bankNodeA) { FindDepositsFlow(externalId = id.externalId!!) }
            .result

        assertEquals(2, deposits.size)
    }
}
