package io.ivno.collateraltoken.workflow.token

import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.workflow.FlowTest
import io.ivno.collateraltoken.workflow.Pipeline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FindTokenTypeFlowTests : FlowTest() {

    private lateinit var ivnoTokenTypeGBP: IvnoTokenType
    private lateinit var ivnoTokenTypeUSD: IvnoTokenType

    override fun initialize() {
        ivnoTokenTypeGBP = IvnoTokenType(NETWORK, custodianParty, tieParty, "GBP", 2)
        ivnoTokenTypeUSD = IvnoTokenType(NETWORK, custodianParty, tieParty, "USD", 2)

        Pipeline
            .create(network)
            .run(tieNode) {
                CreateIvnoTokenTypeFlow.Initiator(ivnoTokenTypeGBP, null, setOf(bankPartyA, bankPartyB))
            }
            .run(tieNode) {
                CreateIvnoTokenTypeFlow.Initiator(ivnoTokenTypeUSD, null, setOf(bankPartyA, bankPartyB))
            }
    }

    @Test
    fun `FindTokenTypeFlow should find the GBP token type by linearId`() {
        val foundTokenType = Pipeline
            .create(network)
            .run(bankNodeA) { FindIvnoTokenTypeFlow(ivnoTokenTypeGBP.linearId) }
            .result!!.state.data

        assertEquals(ivnoTokenTypeGBP, foundTokenType)
    }

    @Test
    fun `FindTokenTypeFlow should find the USD token type by linearId`() {
        val foundTokenType = Pipeline
            .create(network)
            .run(bankNodeA) { FindIvnoTokenTypeFlow(ivnoTokenTypeUSD.linearId) }
            .result!!.state.data

        assertEquals(ivnoTokenTypeUSD, foundTokenType)
    }
}
