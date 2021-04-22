package com.ing.zknotary.testing.fixtures.contract

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.zkp.CircuitMetaData
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction
import java.io.File

public class DummyContract : Contract {
    public companion object {
        public const val PROGRAM_ID: ContractClassName = "com.ing.zknotary.testing.fixtures.contract.DummyContract"
    }

    @Serializable
    public data class Relax(public val now: Boolean = true) : CommandData

    @Serializable
    public class Chill : TypeOnlyCommandData(), ZKCommandData {
        @Transient
        override val circuit: CircuitMetaData = CircuitMetaData(
            // This is just some EXISTING circuit.
            File("${System.getProperty("user.dir")}/../zinc-platform-sources/circuits/create")
        )
    }

    override fun verify(tx: LedgerTransaction) {}
}
