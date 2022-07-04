package com.ing.zkflow.integration.client.flows.upgrade

import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.transactions.LedgerTransaction

class MyContract : Contract {
    companion object {
        val PROGRAM_ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) = Unit
}
