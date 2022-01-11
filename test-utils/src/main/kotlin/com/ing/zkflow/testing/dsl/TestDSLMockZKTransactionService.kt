package com.ing.zkflow.testing.dsl

import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.testing.zkp.MockZKTransactionService
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

public open class TestDSLMockZKTransactionService(serviceHub: ServiceHub) : TestDSLZKTransactionService,
    MockZKTransactionService(serviceHub) {

    public override fun calculatePublicInput(tx: TraversableTransaction): PublicInput = calculatePublicInput(serviceHub, tx)

    override fun run(wtx: WireTransaction) {
        wtx.zkTransactionMetadata().commands.forEach {
            setup(it)
        }
        verify(SignedZKVerifierTransaction(prove(wtx)), false)
    }

    public override fun verify(wtx: WireTransaction, mode: VerificationMode): Unit = run(wtx)
}
