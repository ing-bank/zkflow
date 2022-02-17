package com.ing.zkflow.testing.fixtures.contract

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.transactions.LedgerTransaction

public class DummyContract : Contract {
    public companion object {
        public const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.testing.fixtures.contract.DummyContract"
    }

    public data class Relax(public val now: Boolean = true) : CommandData

    public class Chill : ZKCommandData {

        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            circuit { name = "Chill" }
            numberOfSigners = 2
        }
    }

    override fun verify(tx: LedgerTransaction) {}
}
