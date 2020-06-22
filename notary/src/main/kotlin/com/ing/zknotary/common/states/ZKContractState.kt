package com.ing.zknotary.common.states

import net.corda.core.contracts.ContractState

interface ZKContractState: ContractState, Empty

interface Empty {
    fun empty(): ZKContractState
}
