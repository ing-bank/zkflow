package io.ivno.collateraltoken.workflow.token

import io.ivno.collateraltoken.workflow.FlowTest
import net.corda.core.transactions.SignedTransaction

class UpdateTokenTypeFlowTests : FlowTest() {

    private lateinit var transaction: SignedTransaction

    override fun initialize() {
    }
}
