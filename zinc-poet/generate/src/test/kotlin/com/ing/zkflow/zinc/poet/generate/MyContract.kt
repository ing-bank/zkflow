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
                0 private MyState::class
                1 private MyState::class
                2 private MyOtherState::class
            }
            outputs {
                0 private MyState::class
                1 private MyState::class
                2 private MyState::class
            }
            references {
                0 private MyState::class
                1 private MyState::class
                2 private MyState::class
                3 private MyState::class
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
