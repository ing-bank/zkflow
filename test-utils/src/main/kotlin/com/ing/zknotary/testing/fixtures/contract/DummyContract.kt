package com.ing.zknotary.testing.fixtures.contract

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zknotary.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zknotary.common.zkp.CircuitMetaData
import com.ing.zknotary.testing.fixtures.state.DummyState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.TypeOnlyCommandData
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
        public const val PROGRAM_ID: ContractClassName = "com.ing.zknotary.testing.fixtures.contract.DummyContract"
    }

    @Serializable
    public data class Relax(public val now: Boolean = true) : CommandData

    @Serializable
    public class Chill : TypeOnlyCommandData(), ZKCommandData {
        @Transient
        override val circuit: CircuitMetaData = CircuitMetaData.Builder()
            .name("Chill")
            .addComponentGroupSize(ComponentGroupEnum.SIGNERS_GROUP, 2)
            .build()
    }

    override fun verify(tx: LedgerTransaction) {}
}
