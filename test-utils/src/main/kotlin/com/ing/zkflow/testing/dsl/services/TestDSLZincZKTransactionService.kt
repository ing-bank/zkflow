package com.ing.zkflow.testing.dsl.services

import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZincZKTransactionService
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.testing.dsl.interfaces.VerificationMode
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.WireTransaction

public class TestDSLZincZKTransactionService(serviceHub: ServiceHub) : TestDSLZKTransactionService, ZincZKTransactionService(serviceHub) {
    public override fun calculatePublicInput(tx: ZKVerifierTransaction, commandMetadata: ResolvedZKCommandMetadata): PublicInput =
        calculatePublicInput(serviceHub, tx, commandMetadata)

    override fun run(wtx: WireTransaction): SignedZKVerifierTransaction {
        val proofs = mutableMapOf<String, ByteArray>()
        val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)

        wtx.zkTransactionMetadata().commands.forEach { command ->
            val witness = Witness.fromWireTransaction(
                wtx = wtx,
                inputUtxoInfos = serviceHub.collectUtxoInfos(wtx.inputs),
                referenceUtxoInfos = serviceHub.collectUtxoInfos(wtx.references),
                command
            )
            zkServiceForCommandMetadata(command).run(witness, calculatePublicInput(vtx, command))
        }
        return SignedZKVerifierTransaction(vtx)
    }

    public override fun verify(wtx: WireTransaction, mode: VerificationMode): SignedZKVerifierTransaction {
        return when (mode) {
            VerificationMode.RUN -> run(wtx)
            VerificationMode.PROVE_AND_VERIFY -> {
                wtx.zkTransactionMetadata().commands.forEach {
                    setup(it) // Should be idempotent
                }
                val svtx = SignedZKVerifierTransaction(prove(wtx))
                verify(svtx, false)
                svtx
            }
            VerificationMode.MOCK -> {
                TestDSLMockZKTransactionService(serviceHub).run(wtx)
            }
        }
    }
}
