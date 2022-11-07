package com.example.contract.cbdc

import com.ing.zkflow.common.contracts.ZKContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.NoConstraintPropagation

// Note the hack: we have to implement both `Contract` and `ZKContract`, even though `ZKContract` already extends `Contract`.
// Otherwise, Corda will not add the contract to the transaction as a ContractAttachment in all cases.
class CBDCContract : ZKContract, Contract {
    companion object {
        val ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }
}

