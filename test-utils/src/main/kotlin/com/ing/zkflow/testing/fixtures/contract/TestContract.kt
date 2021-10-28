package com.ing.zkflow.testing.fixtures.contract

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.transactions.zkFLowMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.transactionMetadata
import com.ing.zkflow.serialization.CommandDataSerializerMap
import com.ing.zkflow.serialization.ContractStateSerializerMap
import com.ing.zkflow.testing.fixtures.contract.TestContract.Create.Companion.verifyCreate
import com.ing.zkflow.testing.fixtures.contract.TestContract.Move.Companion.verifyMove
import com.ing.zkflow.testing.fixtures.contract.TestContract.MoveBidirectional.Companion.verifyMoveBidirectional
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import java.io.File
import java.util.Random

public val testSerializers: Unit = run {
    ContractStateSerializerMap.register(TestContract.TestState::class, 1, TestContract.TestState.serializer())
    CommandDataSerializerMap.register(TestContract.Create::class, 2, TestContract.Create.serializer())
    CommandDataSerializerMap.register(TestContract.Move::class, 3, TestContract.Move.serializer())
    CommandDataSerializerMap.register(
        TestContract.MoveBidirectional::class,
        4,
        TestContract.MoveBidirectional.serializer()
    )
    CommandDataSerializerMap.register(TestContract.SignOnly::class, 5, TestContract.SignOnly.serializer())
}

public class TestContract : Contract {
    public companion object {
        public const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.testing.fixtures.contract.TestContract"
    }

    @Serializable
    @BelongsToContract(TestContract::class)
    public data class TestState(
        override val owner: @Contextual AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKOwnableState {
        init {
            // TODO: Hack to trigger the registration of the serializerMap above
            testSerializers
        }

        @FixedLength([2])
        override val participants: List<@Contextual AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(Move(), copy(owner = newOwner))
    }

    // Commands
    @Serializable
    public class Create : TypeOnlyCommandData(), ZKCommandData, ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            commands {
                +Create::class
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            private = true
            circuit {
                buildFolder =
                    File("${System.getProperty("user.dir")}/../zinc-platform-sources/build/circuits/create")
            }
            outputs { 1 of TestState::class }
            numberOfSigners = 1
        }

        public companion object {
            public fun verifyCreate(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                tx.zkFLowMetadata.verify(tx)

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
    public class SignOnly : ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            commands {
                +SignOnly::class
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            private = true
            outputs { 1 of TestState::class }
            numberOfSigners = 2
        }
    }

    @Serializable
    public class Move : ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            commands {
                +Move::class
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            private = true
            circuit {
                buildFolder =
                    File("${System.getProperty("user.dir")}/../zinc-platform-sources/build/circuits/move")
            }
            inputs { 1 of TestState::class }
            outputs { 1 of TestState::class }
            numberOfSigners = 2
        }

        public companion object {
            public fun verifyMove(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                tx.zkFLowMetadata.verify(tx)

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
    public class MoveBidirectional : ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            commands {
                +MoveBidirectional::class
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            private = true
            inputs { 2 of TestState::class }
            outputs { 2 of TestState::class }
            numberOfSigners = 2
        }

        public companion object {
            public fun verifyMoveBidirectional(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                tx.zkFLowMetadata.verify(tx)

                if (tx.inputStates.sumBy { (it as TestState).value } != tx.outputStates.sumBy { (it as TestState).value }) throw IllegalArgumentException(
                    "Failed requirement: amounts are not conserved"
                )

                tx.inputStates.forEachIndexed { index, input ->
                    // Transaction contents
                    val output = tx.getOutput(index) as TestState
                    input as TestState

                    if (input.owner.owningKey == output.owner.owningKey) throw IllegalArgumentException("Failed requirement: input state $index changes ownership")
                    if ((tx.outputStates.reversed()[index] as TestState).owner.owningKey != input.owner.owningKey) throw IllegalArgumentException(
                        "Failed requirement: ownership of input $index should swap ownership"
                    )

                    if (input.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: input state $index is owned by a required command signer")

                    if (input.value != output.value) throw IllegalArgumentException("Failed requirement: the value of the input and out put should be equal")
                }
            }
        }
    }

    override fun verify(tx: LedgerTransaction) {
        // The transaction may have only one command, of a type defined above
        val command = tx.commands.first()

        when (command.value) {
            is Create -> verifyCreate(tx, command)
            is Move -> verifyMove(tx, command)
            is MoveBidirectional -> verifyMoveBidirectional(tx, command)
            is SignOnly -> {
            }
            else -> {
                throw IllegalStateException("No valid command found")
            }
        }
    }
}