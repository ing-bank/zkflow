package com.ing.zkflow.integration.client.flows.upgrade

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.versioning.ZincUpgrade
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import java.util.Random

interface VersionedMyState : VersionedContractStateGroup, ContractState

// STATE VERSIONS
@BelongsToContract(MyContract::class)
@ZKP
data class MyStateV1(
    val holder: @EdDSA AnonymousParty,
    val value: Int = Random().nextInt(1000)
) : VersionedMyState {
    override val participants: List<AbstractParty> = listOf(holder)
}

@BelongsToContract(MyContract::class)
@ZKP
data class MyStateV2(
    val holder: @EdDSA AnonymousParty,
    val value: Long = Random().nextInt(1000).toLong()
) : VersionedMyState {
    override val participants: List<AbstractParty> = listOf(holder)

    @ZincUpgrade(
        upgrade = """
            Self::new(previous.holder, previous.value as i64)
        """,
        additionalChecks = """
             assert!(ctx.signers.contains(input.holder.public_key), "Input owner must sign");
        """
    )
    constructor(previous: MyStateV1) : this(previous.holder, previous.value.toLong())
}

// COMMANDS FOR V1
@ZKP
class CreateV1 : ZKCommandData {
    override val metadata: ResolvedZKCommandMetadata = commandMetadata {
        outputs { private(MyStateV1::class) at 0 }
        numberOfSigners = 1
    }

    override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
    """.trimIndent()
}

// COMMANDS FOR V2
@ZKP
class CreateV2 : ZKCommandData {
    override val metadata: ResolvedZKCommandMetadata = commandMetadata {
        outputs { private(MyStateV2::class) at 0 }
        numberOfSigners = 1
    }

    override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
    """.trimIndent()
}
