package com.ing.zkflow.integration.contract

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import java.util.Random

@SuppressFBWarnings("PREDICTABLE_RANDOM", "PATH_TRAVERSAL_IN", justification = "Test code")
class TestContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.integration.contract.TestContract"
    }

    interface VersionedTestState : VersionedContractStateGroup, OwnableState

    @BelongsToContract(TestContract::class)
    @ZKP
    data class TestState(
        val holder: @EdDSA AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : VersionedTestState {
        override val owner: AbstractParty = holder
        override val participants: List<AbstractParty> = listOf(owner)

        override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
            require(newOwner is AnonymousParty)
            return CommandAndState(MoveAnyToPrivate(), copy(holder = newOwner))
        }
    }

    @ZKP
    class CreatePrivate : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs { private(TestState::class) at 0 }
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

    @ZKP
    class CreatePublic : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                public(TestState::class) at 0
            }
            numberOfSigners = 1
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()

        companion object {
            fun verify(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction contents
                val output = tx.getOutput(0) as TestState
                if (output.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the output state is owned by the command signer")
            }
        }
    }

    /**
     *
     * This command is only used on [CollectSignaturesFlowTest]. It expects two signatures, but nothing else.
     */
    @ZKP
    class SignOnly : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @ZKP
    class MoveAnyToPrivate : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs { any(TestState::class) at 0 }
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @ZKP
    class MovePrivateOnly : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs { private(TestState::class) at 0 }
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @ZKP
    class MoveBidirectional : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs {
                any(TestState::class) at 0
                any(TestState::class) at 1
            }
            outputs {
                private(TestState::class) at 0
                private(TestState::class) at 1
            }
            numberOfSigners = 2
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    override fun verify(tx: LedgerTransaction) {
        // The transaction may have only one command, of a type defined above
        val command = tx.commands.first()

        when (command.value) {

            // Command that have public checks
            is CreatePublic -> CreatePublic.verify(tx, command)

            // command that don't have public checks
            is CreatePrivate, is MoveAnyToPrivate, is MovePrivateOnly, is MoveBidirectional, is SignOnly -> {}
            else -> {
                throw IllegalStateException("No valid command found")
            }
        }
    }
}
