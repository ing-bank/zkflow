package com.ing.zkflow.testing.dsl.services

import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.hasPrivateComponents
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.testing.dsl.interfaces.VerificationMode
import com.ing.zkflow.testing.zkp.MockZKTransactionService
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.WireTransaction

public open class TestDSLMockZKTransactionService(serviceHub: ServiceHub) : TestDSLZKTransactionService,
    MockZKTransactionService(serviceHub) {

    public override fun calculatePublicInput(tx: ZKVerifierTransaction, commandMetadata: ResolvedZKCommandMetadata): PublicInput =
        calculatePublicInput(serviceHub, tx, commandMetadata)

    override fun run(wtx: WireTransaction) {
        if (wtx.hasPrivateComponents) {
            wtx.zkTransactionMetadata().commands.forEach {
                setup(it)
            }
            verify(SignedZKVerifierTransaction(prove(wtx)), false)
        }
    }

    public override fun verify(wtx: WireTransaction, mode: VerificationMode) {
        if (wtx.hasPrivateComponents) {
            run(wtx)
        }
    }
}
