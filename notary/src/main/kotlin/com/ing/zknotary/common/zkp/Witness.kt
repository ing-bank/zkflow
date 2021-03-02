package com.ing.zknotary.common.zkp

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.WireTransaction

// TODO: remove this annotation when it is no longer necessary: when ZincSerialization can also deserialize (for testing)
@CordaSerializable
data class Witness(
    val transaction: WireTransaction,
    val inputs: List<StateAndRef<ContractState>>,
    val references: List<StateAndRef<ContractState>>,
    val inputNonces: List<SecureHash>,
    val referenceNonces: List<SecureHash>
)
