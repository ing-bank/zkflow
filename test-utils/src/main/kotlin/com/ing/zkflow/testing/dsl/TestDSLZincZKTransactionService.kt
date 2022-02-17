package com.ing.zkflow.testing.dsl

import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZincZKTransactionService
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.WireTransaction

public class TestDSLZincZKTransactionService(serviceHub: ServiceHub) : TestDSLZKTransactionService, ZincZKTransactionService(serviceHub) {
    public override fun calculatePublicInput(tx: ZKVerifierTransaction, commandMetadata: ResolvedZKCommandMetadata): PublicInput = calculatePublicInput(serviceHub, tx, commandMetadata)

    override fun run(wtx: WireTransaction) {
        val proofs = mutableMapOf<String, ByteArray>()

        wtx.zkTransactionMetadata().commands.forEach { command ->
            val witness = Witness.fromWireTransaction(
                wtx = wtx,
                inputUtxoInfos = serviceHub.collectUtxoInfos(wtx.inputs),
                referenceUtxoInfos = serviceHub.collectUtxoInfos(wtx.references),
                command
            )

            val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)
            zkServiceForCommandMetadata(command).run(witness, calculatePublicInput(vtx, command))
        }
    }

    public override fun verify(wtx: WireTransaction, mode: VerificationMode) {
        when (mode) {
            VerificationMode.RUN -> {
                run(wtx)
            }
            VerificationMode.PROVE_AND_VERIFY -> {
                wtx.zkTransactionMetadata().commands.forEach {
                    setup(it) // Should be idempotent
                }
                val proof = prove(wtx)
                verify(SignedZKVerifierTransaction(proof), false)
            }
            VerificationMode.MOCK -> {
                TestDSLMockZKTransactionService(serviceHub).run(wtx)
            }
        }
    }
}
