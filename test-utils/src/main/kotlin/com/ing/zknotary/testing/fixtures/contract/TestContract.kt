package com.ing.zknotary.testing.fixtures.contract

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.zkp.CircuitMetaData
import com.ing.zknotary.testing.fixtures.contract.TestContract.Create.Companion.verifyCreate
import com.ing.zknotary.testing.fixtures.contract.TestContract.Move.Companion.verifyMove
import com.ing.zknotary.testing.fixtures.contract.TestContract.MoveBidirectional.Companion.verifyMoveBidirectional
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import java.io.File
import java.util.Random

public class TestContract : Contract {
    public companion object {
        public const val PROGRAM_ID: ContractClassName = "com.ing.zknotary.testing.fixtures.contract.TestContract"
    }

    @BelongsToContract(TestContract::class)
    public data class TestState(
        override val owner: AbstractParty,
        val value: Int = Random().nextInt(1000)
    ) : ContractState, OwnableState {

        override val participants: List<AbstractParty> = listOf(owner)

        override fun withNewOwner(newOwner: AbstractParty): CommandAndState =
            CommandAndState(Move(), copy(owner = newOwner))
    }

    // Commands
    public class Create : ZKCommandData {
        override val id: Int = 0

        override val circuit: CircuitMetaData =
            CircuitMetaData(folder = File("${System.getProperty("user.dir")}/../zinc-platform-sources/circuits/create"))

        public companion object {
            public fun verifyCreate(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                if (tx.outputs.size != 1) throw IllegalArgumentException("Failed requirement: the tx has only one output")
                if (tx.inputs.isNotEmpty()) throw IllegalArgumentException("Failed requirement: the tx has no inputs")

                // Transaction contents
                val output = tx.getOutput(0) as TestState
                if (output.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the output state is owned by the command signer")
            }
        }
    }

    public class Move : ZKCommandData {
        override val id: Int = 1
        override val circuit: CircuitMetaData =
            CircuitMetaData(folder = File("${System.getProperty("user.dir")}/../zinc-platform-sources/circuits/move"))

        public companion object {
            public fun verifyMove(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                if (tx.outputs.size != 1) throw IllegalArgumentException("Failed requirement: the tx has only one output")
                if (tx.inputs.size != 1) throw IllegalArgumentException("Failed requirement: the tx has only one output")

                // Transaction contents
                val output = tx.getOutput(0) as TestState
                val input = tx.getInput(0) as TestState

                if (input.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the input state is owned by a required command signer")
                if (input.value != output.value) throw IllegalArgumentException("Failed requirement: the value of the input and out put should be equal")
            }
        }
    }

    public class MoveBidirectional : ZKCommandData {
        override val id: Int = 1
        override val circuit: CircuitMetaData =
            CircuitMetaData(folder = File(System.getProperty("user.dir")))

        public companion object {
            public fun verifyMoveBidirectional(
                tx: LedgerTransaction,
                command: CommandWithParties<CommandData>
            ) {
                // Transaction structure
                if (tx.outputs.size != 2) throw IllegalArgumentException("Failed requirement: the tx has two outputs")
                if (tx.inputs.size != 2) throw IllegalArgumentException("Failed requirement: the tx has two inputs")

                if (tx.inputStates.sumBy { (it as TestState).value } == tx.outputStates.sumBy { (it as TestState).value }) throw IllegalArgumentException(
                    "Failed requirement: amounts are not conserved"
                )

                tx.inputStates.forEachIndexed { index, input ->
                    // Transaction contents
                    val output = tx.getOutput(index) as TestState
                    input as TestState

                    if (input.owner.owningKey != output.owner.owningKey) throw IllegalArgumentException("Failed requirement: input state $index changes ownership")
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
        if (tx.commands.size != 1) throw IllegalArgumentException("Failed requirement: the tx has only one command")
        val command = tx.commands[0]

        when (command.value) {
            is Create -> verifyCreate(tx, command)
            is Move -> verifyMove(tx, command)
            is MoveBidirectional -> verifyMoveBidirectional(tx, command)
            else -> {
                throw IllegalStateException("No valid command found")
            }
        }
    }
}
