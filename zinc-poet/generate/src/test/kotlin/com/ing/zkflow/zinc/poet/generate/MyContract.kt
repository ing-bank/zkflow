package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.transactionMetadata
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

object MyContract : Contract {
    override fun verify(tx: LedgerTransaction) {}

    class MyFirstCommand : ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            commands {
                +MyFirstCommand::class
            }
        }

        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            private = true
            circuit {
                name = this::class.simpleName!!
            }
            numberOfSigners = 1
            inputs {
                2 of MyState::class
                1 of MyOtherState::class
            }
            outputs {
                3 of MyState::class
            }
            references {
                4 of MyState::class
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
