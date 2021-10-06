package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

interface ZKTransactionService : SerializeAsToken {
    fun prove(wtx: WireTransaction): ZKVerifierTransaction
    fun verify(svtx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean)
    fun validateBackchain(tx: TraversableTransaction)
    fun zkServiceForTransactionMetadata(metadata: ResolvedZKTransactionMetadata): ZKService
}
