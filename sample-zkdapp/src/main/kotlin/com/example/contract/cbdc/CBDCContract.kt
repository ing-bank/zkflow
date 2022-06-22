package com.example.contract.cbdc

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContract
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.transactions.LedgerTransaction

// Note the hack: we have to implement both `Contract` and `ZKContract`, even though `ZKContract` already extends `Contract`.
// Otherwise, Corda will not add the contract to the transaction as a ContractAttachment in all cases.
class CBDCContract : ZKContract, Contract {
    companion object {
        val ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }
}

