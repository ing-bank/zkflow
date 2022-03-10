package com.ing.zkflow.testing.dsl.services

import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
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

    override fun run(wtx: WireTransaction, zkNetworkParameters: ZKNetworkParameters): SignedZKVerifierTransaction {
        wtx.zkTransactionMetadata().commands.forEach { setup(it) }
        val svtx = SignedZKVerifierTransaction(prove(wtx))
        verify(svtx, false)
        return svtx
    }

    public override fun verify(
        wtx: WireTransaction,
        zkNetworkParameters: ZKNetworkParameters,
        mode: VerificationMode
    ): SignedZKVerifierTransaction = run(wtx, zkNetworkParameters)
}
