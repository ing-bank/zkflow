package com.ing.zkflow.testing.fixtures.contract

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zkflow.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zkflow.common.transactions.zkFLowMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.transactionMetadata
import com.ing.zkflow.testing.fixtures.contract.TestMultipleStateContract.Create.Companion.verifyCreate
import com.ing.zkflow.testing.fixtures.contract.TestMultipleStateContract.Move.Companion.verifyMove
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

public object TestMultipleStateContractSerializers {
    init {
        ContractStateSerializerMap.register(TestMultipleStateContract.TestState1::class, 1, TestMultipleStateContract.TestState1.serializer())
        ContractStateSerializerMap.register(TestMultipleStateContract.TestState2::class, 2, TestMultipleStateContract.TestState2.serializer())
        CommandDataSerializerMap.register(TestMultipleStateContract.Create::class, 3, TestMultipleStateContract.Create.serializer())
        CommandDataSerializerMap.register(TestMultipleStateContract.Move::class, 4, TestMultipleStateContract.Move.serializer())
    }
}

public class TestMultipleStateContract : Contract {
    public companion object {
        public const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.testing.fixtures.contract.TestMultipleStateContract"
    }

    @Serializable
    @BelongsToContract(TestMultipleStateContract::class)
    public data class TestState1(
        override val owner: @Contextual AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKOwnableState {

        init {
            /*
             * TODO: This is a hack to ensure that the singleton is initialized. In Kotlin they are lazy until accessed.
             */
            TestMultipleStateContractSerializers
        }

        @FixedLength([2])
        override val participants: List<@Contextual AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(Move(), copy(owner = newOwner))
    }

    @Serializable
    @BelongsToContract(TestMultipleStateContract::class)
    public data class TestState2(
        override val owner: @Contextual AnonymousParty,
        val value: Int = Random().nextInt(1000),
        @FixedLength([2])
        val list: List<Int> = listOf()
    ) : ZKOwnableState {

        init {
            /*
             * TODO: This is a hack to ensure that the singleton is initialized. In Kotlin they are lazy until accessed.
             */
            TestMultipleStateContractSerializers
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
                +TestMultipleStateContract.Create::class
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            private = true
            circuit {
                buildFolder =
                    File("${System.getProperty("user.dir")}/../zinc-platform-sources/build/circuits/create-multi-state")
            }
            outputs {
                1 of TestMultipleStateContract.TestState1::class
                1 of TestMultipleStateContract.TestState2::class
            }
            numberOfSigners = 1
        }

        public companion object {
            public fun verifyCreate(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                tx.zkFLowMetadata.verify(tx)
                // Transaction structure
                if (tx.outputs.size != 2) throw IllegalArgumentException("Failed requirement: the tx has exactly two outputs")
                if (tx.inputs.isNotEmpty()) throw IllegalArgumentException("Failed requirement: the tx has no inputs")

                // Transaction contents
                val output1 = tx.getOutput(0) as TestState1
                val output2 = tx.getOutput(1) as TestState2
                if (output1.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the first output state is owned by the command signer")
                if (output2.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the second output state is owned by the command signer")
            }
        }
    }

    @Serializable
    public class Move : TypeOnlyCommandData(), ZKCommandData, ZKTransactionMetadataCommandData {

        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            commands {
                +TestMultipleStateContract.Move::class
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            private = true
            circuit {
                buildFolder =
                    File("${System.getProperty("user.dir")}/../zinc-platform-sources/build/circuits/move-multi-state")
            }
            inputs {
                1 of TestMultipleStateContract.TestState1::class
                1 of TestMultipleStateContract.TestState2::class
            }
            outputs {
                1 of TestMultipleStateContract.TestState1::class
                1 of TestMultipleStateContract.TestState2::class
            }
            numberOfSigners = 2
        }
        public companion object {
            public fun verifyMove(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                tx.zkFLowMetadata.verify(tx)
                // Transaction structure
                if (tx.outputs.size != 2) throw IllegalArgumentException("Failed requirement: the tx has exactly two outputs")
                if (tx.inputs.size != 2) throw IllegalArgumentException("Failed requirement: the tx has exactly two inputs")

                // Transaction contents
                val output1 = tx.getOutput(0) as TestState1
                val output2 = tx.getOutput(1) as TestState2

                val input1 = tx.getInput(0) as TestState1
                val input2 = tx.getInput(1) as TestState2

                if (input1.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the first input state is owned by a required command signer")
                if (input2.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the second input state is owned by a required command signer")
                if (input1.value != output1.value) throw IllegalArgumentException("Failed requirement: the value of the input and output should be equal")
                if (input2.value != output2.value) throw IllegalArgumentException("Failed requirement: the value of the input and output should be equal")
            }
        }
    }

    override fun verify(tx: LedgerTransaction) {
        // The transaction may have only one command, of a type defined above
        if (tx.commands.size != 1) throw IllegalArgumentException("Failed requirement: the tx has only one command")
        val command = tx.commands[0]

        when (command.value) {
            is Create -> verifyCreate(tx, command)
            is Move -> verifyMove(tx, command)
            else -> {
                throw IllegalStateException("No valid command found")
            }
        }
    }
}
