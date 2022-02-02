package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

object MyContract : Contract {
    override fun verify(tx: LedgerTransaction) {}

    class MyFirstCommand : ZKCommandData {

        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            circuit {
                name = this::class.simpleName!!
            }
            numberOfSigners = 1
            inputs {
                private(MyState::class) at 0
                private(MyState::class) at 1
                private(MyOtherState::class) at 2
            }
            outputs {
                MyState::class at 0
                MyState::class at 1
                MyState::class at 2
            }
            references {
                private(MyState::class) at 0
                private(MyState::class) at 1
                private(MyState::class) at 2
                private(MyState::class) at 3
            }
        }
    }
}

@ZKP
@BelongsToContract(MyContract::class)
data class MyState(
    val ageInYears: Int
) : ContractState {
    override val participants: List<AbstractParty> = emptyList()
}

@ZKP
@BelongsToContract(MyContract::class)
data class MyOtherState(
    val ageInYears: Int
) : ContractState {
    override val participants: List<AbstractParty> = emptyList()
}
