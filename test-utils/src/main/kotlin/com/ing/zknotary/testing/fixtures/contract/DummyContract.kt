package com.ing.zknotary.testing.fixtures.contract

import kotlinx.serialization.Serializable
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

public class DummyContract : Contract {
    public companion object {
        public const val PROGRAM_ID: ContractClassName = "com.ing.zknotary.testing.fixtures.contract.DummyContract"
    }

    @Serializable
    public class Relax(public val now: Boolean = true) : CommandData

    @Serializable
    public class Chill : TypeOnlyCommandData()

    override fun verify(tx: LedgerTransaction) {}
}
