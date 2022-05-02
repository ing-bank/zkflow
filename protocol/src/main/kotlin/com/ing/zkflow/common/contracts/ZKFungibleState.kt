package com.ing.zkflow.common.contracts

import net.corda.core.contracts.FungibleState

interface ZKFungibleState<T : Any> : FungibleState<T>, ZKContractState
