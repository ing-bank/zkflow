package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransactionWithoutProofs
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

interface ZKTransactionService : SerializeAsToken {
    val vtxStorage: ZKVerifierTransactionStorage
    fun setup(command: ResolvedZKCommandMetadata, force: Boolean = false)
    fun prove(wtx: WireTransaction): ZKVerifierTransaction
    fun verify(vtx: ZKVerifierTransaction)

    /**
     * This should not be called for actual proof verification, only in situations where we 'run' the circuit for correctness
     */
    fun verify(wtx: WireTransaction): ZKVerifierTransactionWithoutProofs
    fun validateBackchain(tx: TraversableTransaction)
    fun zkServiceForCommandMetadata(metadata: ResolvedZKCommandMetadata): ZKService
}
