package com.ing.zkflow.testing.fixtures.contract

import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.transactionMetadata
import com.ing.zkflow.serialization.CommandDataSerializerMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.transactions.LedgerTransaction

public class DummyContract : Contract {
    public companion object {
        public const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.testing.fixtures.contract.DummyContract"
    }

    @Serializable
    public data class Relax(public val now: Boolean = true) : CommandData {
        init {
            CommandDataSerializerMap.register(this::class)
        }
    }

    @Serializable
    public class Chill : ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            commands { +Chill::class }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            circuit { name = "Chill" }
            numberOfSigners = 2
        }

        init {
            CommandDataSerializerMap.register(this::class)
        }
    }

    override fun verify(tx: LedgerTransaction) {}
}
