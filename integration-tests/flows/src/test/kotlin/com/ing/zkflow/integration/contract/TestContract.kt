package com.ing.zkflow.integration.contract

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.serialization.BFLSerializationScheme.Companion.ZkCommandDataSerializerMap
import com.ing.zkflow.common.serialization.BFLSerializationScheme.Companion.ZkContractStateSerializerMap
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.integration.contract.TestContract.Create.Companion.verifyCreate
import com.ing.zkflow.integration.contract.TestContract.Move.Companion.verifyMove
import com.ing.zkflow.integration.contract.TestContract.MoveBidirectional.Companion.verifyMoveBidirectional
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import java.util.Random

/*
 * TODO: When the new BFLSerializationScheme is in place, this class can be replaced with TestContractWithZKPAnnotations.
 * TestContractWithZKPAnnotations can then be renamed to TestContract
 */
@SuppressFBWarnings("PREDICTABLE_RANDOM", "PATH_TRAVERSAL_IN", justification = "Test code")
class TestContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.integration.contract.TestContract"
    }

    @Serializable
    @BelongsToContract(TestContract::class)
    data class TestState(
        override val owner: @Contextual AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKOwnableState {
        init {
            ZkContractStateSerializerMap.register(this::class)
        }

        companion object {
            const val PARTICIPANT_COUNT: Int = 1
        }

        @FixedLength([PARTICIPANT_COUNT])
        override val participants: List<@Contextual AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(Move(), copy(owner = newOwner))
    }

    // Commands
    @Serializable
    class Create : ZKCommandData {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 1
        }

        init {
            ZkCommandDataSerializerMap.register(this::class)
        }

        companion object {
            fun verifyCreate(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                tx.zkTransactionMetadata().verify(tx)

                // Transaction contents
                val output = tx.getOutput(0) as TestState
                if (output.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the output state is owned by the command signer")
            }
        }
    }

    @Serializable
    class CreatePublic : ZKCommandData {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                public(TestState::class) at 0
            }
            numberOfSigners = 1
        }

        init {
            ZkCommandDataSerializerMap.register(this::class)
        }

        companion object {
            fun verify(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                tx.zkTransactionMetadata().verify(tx)

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
    @Serializable
    class SignOnly : ZKCommandData {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }

        init {
            ZkCommandDataSerializerMap.register(this::class)
        }
    }

    @Serializable
    class Move : ZKCommandData {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs { any(TestState::class) at 0 }
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }

        init {
            ZkCommandDataSerializerMap.register(this::class)
        }

        companion object {
            fun verifyMove(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                tx.zkTransactionMetadata().verify(tx)

                // Transaction contents
                val output = tx.getOutput(0) as TestState
                val input = tx.getInput(0) as TestState

                if (input.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the input state is owned by a required command signer")
                if (output.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the outputs state is owned by a required command signer")
                if (input.value != output.value) throw IllegalArgumentException("Failed requirement: the value of the input and out put should be equal")
            }
        }
    }

    @Serializable
    class MovePrivateOnly : ZKCommandData {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs { private(TestState::class) at 0 }
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }

        init {
            ZkCommandDataSerializerMap.register(this::class)
        }

        companion object {
            fun verify(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                tx.zkTransactionMetadata().verify(tx)

                // Transaction contents
                val output = tx.getOutput(0) as TestState
                val input = tx.getInput(0) as TestState

                if (input.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the input state is owned by a required command signer")
                if (output.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the outputs state is owned by a required command signer")
                if (input.value != output.value) throw IllegalArgumentException("Failed requirement: the value of the input and out put should be equal")
            }
        }
    }

    @Serializable
    class MoveBidirectional : ZKCommandData {

        @Transient
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

        init {
            ZkCommandDataSerializerMap.register(this::class)
        }

        companion object {
            fun verifyMoveBidirectional(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                tx.zkTransactionMetadata().verify(tx)

                // Transaction contents
                if (tx.inputsOfType<TestState>().sumBy { it.value } != tx.outputsOfType<TestState>()
                    .sumBy { it.value }
                ) throw IllegalArgumentException(
                    "Failed requirement: amounts are not conserved for TestState"
                )

                tx.inputStates.forEachIndexed { index, input ->
                    val output = tx.getOutput(index) as ZKOwnableState
                    input as ZKOwnableState

                    if (input.owner.owningKey == output.owner.owningKey) throw IllegalArgumentException("Failed requirement: input state $index changes ownership")
                    if ((tx.outputStates.reversed()[index] as ZKOwnableState).owner.owningKey != input.owner.owningKey) throw IllegalArgumentException(
                        "Failed requirement: ownership of input $index should swap ownership"
                    )

                    if (input.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: input state $index is owned by a required command signer")
                }
            }
        }
    }

    override fun verify(tx: LedgerTransaction) {
        // The transaction may have only one command, of a type defined above
        val command = tx.commands.first()

        when (command.value) {
            is Create -> verifyCreate(tx, command)
            is CreatePublic -> CreatePublic.verify(tx, command)
            is Move -> verifyMove(tx, command)
            is MovePrivateOnly -> MovePrivateOnly.verify(tx, command)
            is MoveBidirectional -> verifyMoveBidirectional(tx, command)
            is SignOnly -> {
            }
            else -> {
                throw IllegalStateException("No valid command found")
            }
        }
    }
}
