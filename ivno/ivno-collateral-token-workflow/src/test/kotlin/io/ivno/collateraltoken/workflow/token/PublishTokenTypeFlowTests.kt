package io.ivno.collateraltoken.workflow.token

import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.workflow.FlowTest
import io.ivno.collateraltoken.workflow.Pipeline
import io.ivno.collateraltoken.workflow.singleOutputOfType
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class PublishTokenTypeFlowTests : FlowTest() {
    private lateinit var transaction: SignedTransaction

    override fun initialize() {
        Pipeline
            .create(network)
            .run(tieNode) {
                CreateIvnoTokenTypeFlow.Initiator(TOKEN_TYPE, null, setOf(bankPartyA, custodianParty))
            }
            .run(tieNode) {
                val tokenType = it.singleOutputOfType<IvnoTokenType>()
                PublishIvnoTokenTypeFlow.Initiator(tokenType.linearId, setOf(bankPartyB))
            }
            .finally {
                transaction = it
            }
    }

    @Test
    fun `PublishTokenTypeFlow should record a transaction for the new observers`() {
        val recordedTransaction = bankNodeB.services.validatedTransactions.getTransaction(transaction.id)
            ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")
        assertEquals(recordedTransaction, transaction, "Transactions are not equal.")
    }

    @Test
    fun `PublishTokenTypeFlow should record a token type state for the new observers`() {
        val txState = transaction.tx.outRefsOfType<IvnoTokenType>().singleOrNull()
            ?: fail("Failed to find Ivno token type")
        val recordedState =
            bankNodeB.services.vaultService.queryBy(
                IvnoTokenType::class.java,
                QueryCriteria.LinearStateQueryCriteria(
                    linearId = listOf(txState.state.data.linearId)
                )
            ).states.singleOrNull()
        assertEquals(txState, recordedState)
    }
}
