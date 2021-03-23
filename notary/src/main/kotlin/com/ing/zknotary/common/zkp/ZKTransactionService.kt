package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import net.corda.core.contracts.StateRef
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction

interface ZKTransactionService : SerializeAsToken {
    fun prove(
        wtx: WireTransaction
    ): ZKVerifierTransaction

    fun verify(stx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean)
    fun validateBackchain(tx: TraversableTransaction)
}
