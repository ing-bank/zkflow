package com.ing.zkflow.testing.fixtures.contract

import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zkflow.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.transactionMetadata
import com.ing.zkflow.testing.fixtures.state.DummyState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.transactions.LedgerTransaction

public object DummySerializers {
    init {
        ContractStateSerializerMap.register(DummyState::class, 9999, DummyState.serializer())
        CommandDataSerializerMap.register(DummyContract.Relax::class, 9998, DummyContract.Relax.serializer())
        CommandDataSerializerMap.register(DummyContract.Chill::class, 9997, DummyContract.Chill.serializer())
    }
}

public class DummyContract : Contract {
    public companion object {
        public const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.testing.fixtures.contract.DummyContract"
    }

    @Serializable
    public data class Relax(public val now: Boolean = true) : CommandData

    @Serializable
    public class Chill : ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            commands { +Chill::class }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            private = true
            circuit { name = "Chill" }
            numberOfSigners = 2
        }
    }

    override fun verify(tx: LedgerTransaction) {}
}
