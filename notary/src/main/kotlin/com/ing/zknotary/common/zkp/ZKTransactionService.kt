package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.WireTransaction

@CordaSerializable
class UtxoInfo(
    val stateRef: StateRef,
    val serializedContents: ByteArray,
    val nonce: SecureHash,
    val digestAlgorithm: String
)

interface ZKTransactionService : SerializeAsToken {
    fun prove(
        wtx: WireTransaction,
        receivedStateInfo: List<UtxoInfo> = emptyList()
    ): ZKVerifierTransaction

    fun verify(stx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean)
}
