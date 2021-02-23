package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.WireTransaction

interface ZKTransactionService : SerializeAsToken {
    fun toZKProverTransaction(tx: WireTransaction): ZKProverTransaction
    fun prove(ptx: ZKProverTransaction): ZKVerifierTransaction
    fun verify(stx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean)
}
