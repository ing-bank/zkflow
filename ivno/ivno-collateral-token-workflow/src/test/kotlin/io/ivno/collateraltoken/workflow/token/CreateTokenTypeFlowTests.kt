package io.ivno.collateraltoken.workflow.token

import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.workflow.FlowTest
import io.ivno.collateraltoken.workflow.Pipeline
import io.ivno.collateraltoken.workflow.singleOutRefOfType
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.SignedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals

class CreateTokenTypeFlowTests : FlowTest() {

    private lateinit var transaction: SignedTransaction
    private lateinit var tokenType: StateAndRef<IvnoTokenType>

    override fun initialize() {
        Pipeline
            .create(network)
            .run(tieNode) {
                CreateIvnoTokenTypeFlow.Initiator(TOKEN_TYPE, null, TOKEN_TYPE_OBSERVERS)
            }
            .finally {
                transaction = it
                tokenType = transaction.singleOutRefOfType()
            }
    }

    @Test
    fun `CreateTokenTypeFlow should be signed by the custodian`() {
        transaction.verifyRequiredSignatures()
    }

    @Test
    fun `CreateTokenTypeFlow should record a transaction for the token issuing entity, custodian and all observers`() {
        listOf(tieNode, custodianNode, bankNodeA, bankNodeB).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                assertEquals(transaction, recordedTransaction, "Transactions are not equal.")
            }
        }
    }

    @Test
    fun `CreateTokenTypeFlow should record a token type state for the token issuing entity, custodian and all observers`() {
        listOf(tieNode, custodianNode, bankNodeA, bankNodeB).forEach {
            it.transaction {
                val recordedTransaction = it.services.validatedTransactions.getTransaction(transaction.id)
                    ?: fail("Failed to find a recorded transaction with ID: ${transaction.id}.")

                val recordedTokenType = recordedTransaction.tx.outputsOfType<IvnoTokenType>().singleOrNull()
                    ?: fail("Failed to find a token state in the recorded transaction.")

                assertEquals(recordedTokenType, tokenType.state.data, "Deposits are not equal.")
                assertEquals(recordedTokenType.displayName, "GBP", "Currency should be GBP.")
                assertEquals(recordedTokenType.fractionDigits, 2, "Fraction digits should be 2.")
                assertEquals(recordedTokenType.issuer, tieParty, "Issuer should be tie.")
            }
        }
    }
}
