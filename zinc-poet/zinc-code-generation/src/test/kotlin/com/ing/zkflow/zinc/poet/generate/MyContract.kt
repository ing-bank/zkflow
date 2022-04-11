package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.versioning.ZincUpgrade
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

object MyContract : Contract {
    override fun verify(tx: LedgerTransaction) {}

    @ZKP
    class MyFirstCommand : ZKCommandData {

        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            circuit {
                name = this::class.simpleName!!
            }
            numberOfSigners = 2
            command = true
            notary = true
            timeWindow = true
            networkParameters = true
            inputs {
                any(MyStateV2::class) at 0
                any(MyStateV2::class) at 1
                any(MyOtherState::class) at 2
            }
            outputs {
                private(MyStateV2::class) at 0
                private(MyStateV2::class) at 1
                private(MyStateV2::class) at 2
            }
            references {
                any(MyStateV2::class) at 0
                any(MyStateV2::class) at 1
                any(MyStateV2::class) at 2
                any(MyStateV2::class) at 3
            }
        }
    }

    // TODO This should at some point be automatically generated
    @ZKP
    class UpgradeMyStateV1ToMyStateV2 : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            circuit {
                name = this::class.simpleName!!
            }
            numberOfSigners = 1
            command = true
            notary = true
            inputs {
                private(MyStateV1::class) at 0
            }
            outputs {
                private(MyStateV2::class) at 0
            }
        }

        override fun verifyPrivate(): String = generateUpgradeVerification(metadata).generate()
    }
}

interface MyStateI : Versioned, ContractState

@ZKP
@BelongsToContract(MyContract::class)
data class MyStateV1(
    val ageInYears: Int
) : MyStateI {
    override val participants: List<AbstractParty> = emptyList()
}

@ZKP
@BelongsToContract(MyContract::class)
data class MyStateV2(
    val ageInYears: Int,
    val count: Int,
) : MyStateI {
    @ZincUpgrade("Self::new(previous_state.age_in_years, 1 as i32)")
    constructor(previousState: MyStateV1) : this(previousState.ageInYears, 1)

    override val participants: List<AbstractParty> = emptyList()
}

@ZKP
@BelongsToContract(MyContract::class)
data class MyOtherState(
    val ageInYears: Int
) : ContractState {
    override val participants: List<AbstractParty> = emptyList()
}
