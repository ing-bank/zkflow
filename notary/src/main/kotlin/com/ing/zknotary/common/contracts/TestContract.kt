package com.ing.zknotary.common.contracts

import com.ing.zknotary.common.zkp.fingerprint
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import java.util.Random

class TestContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.ing.zknotary.common.contracts.TestContract"
    }

    @BelongsToContract(TestContract::class)
    data class TestState(
        override val owner: AbstractParty,
        val value: Int = Random().nextInt(1000)
    ) :
        ZKContractState, OwnableState {
        override val participants = listOf(owner)
        override fun withNewOwner(newOwner: AbstractParty) = CommandAndState(Move(), copy(owner = newOwner))

        /**
         * TODO: Try to find an automatable way of generating the fingerprint from arbitrary objects,
         * that is repeatable in Zinc.
         * The benefit is that users need not implement the Fingerprintable interface.
         */
        override val fingerprint: ByteArray =
            nonce.fingerprint + owner.fingerprint + value.fingerprint
    }

    // Commands
    class Create : ZKCommandData {
        override val fingerprint: ByteArray
            get() = ByteArray(1) { 0 }
    }

    class Move : ZKCommandData {
        override val fingerprint: ByteArray
            get() = ByteArray(1) { 1 }
    }

    override fun verify(tx: LedgerTransaction) {
        // The transaction may have only one command, of a type defined above
        if (tx.commands.size != 1) throw IllegalArgumentException("Failed requirement: the tx has only one command")
        val command = tx.commands[0]

        when (command.value) {
            is Create -> {
                // Transaction structure
                if (tx.outputs.size != 1) throw IllegalArgumentException("Failed requirement: the tx has only one output")
                if (tx.inputs.isNotEmpty()) throw IllegalArgumentException("Failed requirement: the tx has no inputs")

                // Transaction contents
                val output = tx.getOutput(0) as TestState
                if (output.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the output state is owned by the command signer")
            }
            is Move -> {
                // Transaction structure
                if (tx.outputs.size != 1) throw IllegalArgumentException("Failed requirement: the tx has only one output")
                if (tx.inputs.size != 1) throw IllegalArgumentException("Failed requirement: the tx has only one output")

                // Transaction contents
                val output = tx.getOutput(0) as TestState
                val input = tx.getInput(0) as TestState

                /*
                // Note: the fact that command.signers contains a certain required key, does not mean we can assume it has been
                // verified that this signature is present. The validating notary does check this directly after the contract verification,
                // but the non-validating notary never checks signatures. In that case, this check only means that we
                // can enforce that the owner of e.g. the output is set as one of the required signers by the tx creator,
                // but not that these signatures are actually present.
                // Counterparties also do contract verification, and like a validating notary, do check signatures.
                // In that case, this check equals saying that we require a signature to be present on the tx of the
                // owner of the input and of the owner of the output.

                 */
                if (input.owner.owningKey !in command.signers) throw IllegalArgumentException("Failed requirement: the input state is owned by a required command signer")
                if (input.value != output.value) throw IllegalArgumentException("Failed requirement: the value of the input and out put should be equal")
            }
            else -> {
                throw IllegalStateException("No valid command found")
            }
        }
    }
}
