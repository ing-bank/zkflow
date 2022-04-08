package com.ing.zkflow.integration.contract

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.common.serialization.BFLSerializationScheme.Companion.CommandDataSerializerRegistry
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.integration.contract.TestContractOld.Create.Companion.verifyCreate
import com.ing.zkflow.integration.contract.TestContractOld.Move.Companion.verifyMove
import com.ing.zkflow.integration.contract.TestContractOld.MoveBidirectional.Companion.verifyMoveBidirectional
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
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
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import java.util.Random

/*
 * TODO: When the new BFLSerializationScheme is in place, this class can be replaced with TestContractWithZKPAnnotations.
 * TestContractWithZKPAnnotations can then be renamed to TestContract
 */
@SuppressFBWarnings("PREDICTABLE_RANDOM", "PATH_TRAVERSAL_IN", justification = "Test code")
class TestContractOld : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.integration.contract.TestContract"
    }

    interface TestStateInterfaceOld : Versioned
    @Serializable
    @BelongsToContract(TestContract::class)
    data class TestState(
        override val owner: @Serializable(with = OwnerSerializer::class) AnonymousParty,
        val value: @Serializable(with = IntSerializer::class) Int = Random().nextInt(1000)
    ) : ZKOwnableState, TestStateInterfaceOld {
        private object OwnerSerializer : AnonymousPartySerializer(Crypto.EDDSA_ED25519_SHA512.schemeNumberID)

        init {
            BFLSerializationScheme.Companion.ContractStateSerializerRegistry.register(this::class, serializer())
        }

        @Transient
        override val participants: List<@Contextual AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState = CommandAndState(Move(), copy(owner = newOwner))
    }

    // Commands
    interface CreateInterfaceOld : Versioned
    @Serializable
    class Create : ZKCommandData, CreateInterfaceOld {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 1
        }

        init {
            CommandDataSerializerRegistry.register(this::class, serializer())
        }

        companion object {
            fun verifyCreate(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction contents
                val output = tx.getOutput(0) as TestState
                if (output.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the output state is owned by the command signer")
            }
        }
    }

    interface CreatePublicInterfaceOld : Versioned
    @Serializable
    class CreatePublic : ZKCommandData, CreatePublicInterfaceOld {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                public(TestState::class) at 0
            }
            numberOfSigners = 1
        }

        init {
            CommandDataSerializerRegistry.register(this::class, Create.serializer())
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
    interface SignOnlyInterfaceOld : Versioned
    @Serializable
    class SignOnly : ZKCommandData, SignOnlyInterfaceOld {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }

        init {
            CommandDataSerializerRegistry.register(this::class, Create.serializer())
        }
    }

    interface MoveInterfaceOld : Versioned
    @Serializable
    class Move : ZKCommandData, MoveInterfaceOld {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs { any(TestState::class) at 0 }
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }

        init {
            CommandDataSerializerRegistry.register(this::class, Create.serializer())
        }

        companion object {
            fun verifyMove(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction contents
                val output = tx.getOutput(0) as TestState
                val input = tx.getInput(0) as TestState

                if (input.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the input state is owned by a required command signer")
                if (output.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the outputs state is owned by a required command signer")
                if (input.value != output.value) throw IllegalArgumentException("Failed requirement: the value of the input and out put should be equal")
            }
        }
    }

    interface MovePrivateOnlyInterfaceOld : Versioned
    @Serializable
    class MovePrivateOnly : ZKCommandData, MovePrivateOnlyInterfaceOld {

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs { private(TestState::class) at 0 }
            outputs { private(TestState::class) at 0 }
            numberOfSigners = 2
        }

        init {
            CommandDataSerializerRegistry.register(this::class, Create.serializer())
        }

        companion object {
            fun verify(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction contents
                val output = tx.getOutput(0) as TestState
                val input = tx.getInput(0) as TestState

                if (input.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the input state is owned by a required command signer")
                if (output.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the outputs state is owned by a required command signer")
                if (input.value != output.value) throw IllegalArgumentException("Failed requirement: the value of the input and out put should be equal")
            }
        }
    }

    interface MoveBidirectionalInterfaceOld : Versioned
    @Serializable
    class MoveBidirectional : ZKCommandData, MoveBidirectionalInterfaceOld {

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
            CommandDataSerializerRegistry.register(this::class, Create.serializer())
        }

        companion object {
            fun verifyMoveBidirectional(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction contents
                if (tx.inputsOfType<TestState>().sumBy { it.value } != tx.outputsOfType<TestState>().sumBy { it.value }
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
