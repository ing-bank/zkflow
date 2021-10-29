package com.ing.zkflow.testing.dsl

import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.zkp.PublicInput
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes

public interface TestDSLZKTransactionService {
    /**
     * Called to execute setup, prove and verify in one go
     */
    public fun run(wtx: WireTransaction)
    public fun verify(wtx: WireTransaction, mode: VerificationMode)

    public fun calculatePublicInput(serviceHub: ServiceHub, tx: TraversableTransaction): PublicInput {
        val inputHashes =
            serviceHub.collectUtxoInfos(tx.inputs)
                .map { tx.digestService.componentHash(it.nonce, OpaqueBytes(it.serializedContents)) }
        val referenceHashes =
            serviceHub.collectUtxoInfos(tx.references)
                .map { tx.digestService.componentHash(it.nonce, OpaqueBytes(it.serializedContents)) }

        return PublicInput(
            transactionId = tx.id,
            inputHashes = inputHashes,
            referenceHashes = referenceHashes
        )
    }
}
