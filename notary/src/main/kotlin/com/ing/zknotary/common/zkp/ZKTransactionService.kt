package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import net.corda.core.serialization.SerializeAsToken

interface ZKTransactionService : SerializeAsToken {
    fun prove(witness: Witness): ByteArray
    fun verify(proof: ByteArray, publicInput: PublicInput)
    fun verify(tx: ZKVerifierTransaction)
}
