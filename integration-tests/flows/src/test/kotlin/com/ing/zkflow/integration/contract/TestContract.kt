package com.ing.zkflow.integration.contract

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import java.util.Random

@SuppressFBWarnings("PREDICTABLE_RANDOM", "PATH_TRAVERSAL_IN", justification = "Test code")
class TestContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.integration.contract.TestContract"
    }

    interface VersionedTestState : Versioned, ZKOwnableState

    @BelongsToContract(TestContract::class)
    @ZKP
    data class TestState(
        override val owner: @EdDSA AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : VersionedTestState {
        override val participants: List<AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(MoveAnyToPrivate(), copy(owner = newOwner))
    }

    // Commands
    interface VersionedCreateCommandData : Versioned, ZKCommandData

    @ZKP
    class CreatePrivate : VersionedCreateCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 1
        }
    }

    interface CreatePublicCommandData : Versioned, ZKCommandData

    @ZKP
    class CreatePublic : CreatePublicCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                public(TestState::class) at 0
            }
            numberOfSigners = 1
        }

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
    interface SignOnlyCommandData : Versioned, ZKCommandData

    @ZKP
    class SignOnly : SignOnlyCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }
    }

    interface MoveAnyToPrivateCommandData : Versioned, ZKCommandData

    @ZKP
    class MoveAnyToPrivate : MoveAnyToPrivateCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs { any(TestState::class) at 0 }
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }
    }

    interface MovePrivateOnlyCommandData : Versioned, ZKCommandData

    @ZKP
    class MovePrivateOnly : MovePrivateOnlyCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs { private(TestState::class) at 0 }
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }
    }

    interface MoveBidirectionalCommandData : Versioned, ZKCommandData

    @ZKP
    class MoveBidirectional : MoveBidirectionalCommandData {
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
