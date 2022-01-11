@file:Suppress("MagicNumber") // This is actually test code, but for platform sources needs to be in main
package com.ing.zkflow.contract

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.contract.TestMultipleStateContract.Create.Companion.verifyCreate
import com.ing.zkflow.contract.TestMultipleStateContract.Move.Companion.verifyMove
import com.ing.zkflow.serialization.CommandDataSerializerMap
import com.ing.zkflow.serialization.ContractStateSerializerMap
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
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

class TestMultipleStateContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.ing.zkflow.contract.TestMultipleStateContract"
    }

    @Serializable
    @BelongsToContract(TestMultipleStateContract::class)
    data class TestState1(
        override val owner: @Contextual AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKOwnableState {

        init {
            ContractStateSerializerMap.register(this::class)
        }

        @FixedLength([1])
        override val participants: List<@Contextual AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(Move(), copy(owner = newOwner))
    }

    @Serializable
    @BelongsToContract(TestMultipleStateContract::class)
    data class TestState2(
        override val owner: @Contextual AnonymousParty,
        val value: Int = Random().nextInt(1000),
        @FixedLength([2])
        val list: List<Int> = listOf()
    ) : ZKOwnableState {

        init {
            ContractStateSerializerMap.register(this::class)
        }

        @FixedLength([1])
        override val participants: List<@Contextual AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(Move(), copy(owner = newOwner))
    }

    // Commands
    @Serializable
    class Create : TypeOnlyCommandData() {
        init {
            CommandDataSerializerMap.register(this::class)
        }

        companion object {
            fun verifyCreate(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // tx.zkFLowMetadata.verify(tx)

                // Transaction contents
                val output1 = tx.getOutput(0) as TestState1
                val output2 = tx.getOutput(1) as TestState2
                if (output1.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the first output state is owned by the command signer")
                if (output2.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the second output state is owned by the command signer")
            }
        }
    }

    @Serializable
    class Move : ZKCommandData {
        init {
            CommandDataSerializerMap.register(this::class)
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            network {
                attachmentConstraintType = AlwaysAcceptAttachmentConstraint::class // to simplify DSL tests
            }
            circuit {
                name = "move_multi_state"
                buildFolder =
                    File("${System.getProperty("user.dir")}/../zinc-platform-sources/build/circuits/move_multi_state")
            }
            privateInputs {
                1 private TestState1::class
                1 private TestState2::class
            }
            privateOutputs {
                1 private TestState1::class
                1 private TestState2::class
            }
            numberOfSigners = 2
        }

        companion object {
            fun verifyMove(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                tx.zkTransactionMetadata().verify(tx)

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
