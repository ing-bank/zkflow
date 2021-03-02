package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.WireTransaction

interface ZKTransactionService : SerializeAsToken {
    fun prove(wtx: WireTransaction, inputs: List<StateAndRef<ContractState>>, references: List<StateAndRef<ContractState>>): ZKVerifierTransaction
    fun verify(stx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean)
}
