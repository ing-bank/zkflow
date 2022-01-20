package com.ing.zkflow.testing.dsl

import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.transactions.zkTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZincZKTransactionService
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

public class TestDSLZincZKTransactionService(serviceHub: ServiceHub) : TestDSLZKTransactionService, ZincZKTransactionService(serviceHub) {
    public override fun calculatePublicInput(tx: TraversableTransaction): PublicInput = calculatePublicInput(serviceHub, tx)

    override fun run(wtx: WireTransaction) {
        val witness = Witness.fromWireTransaction(
            wtx,
            serviceHub.collectUtxoInfos(wtx.inputs),
            serviceHub.collectUtxoInfos(wtx.references)
        )
        zkServiceForTransactionMetadata(wtx.zkTransactionMetadata()).run(witness, calculatePublicInput(wtx))
    }

    public override fun verify(wtx: WireTransaction, mode: VerificationMode) {
        when (mode) {
            VerificationMode.RUN -> {
                run(wtx)
            }
            VerificationMode.PROVE_AND_VERIFY -> {
                setup(wtx.zkTransactionMetadataCommandData) // Should be idempotent
                val proof = prove(wtx)
                verify(SignedZKVerifierTransaction(proof), false)
            }
            VerificationMode.MOCK -> {
                TestDSLMockZKTransactionService(serviceHub).run(wtx)
            }
        }
    }
}