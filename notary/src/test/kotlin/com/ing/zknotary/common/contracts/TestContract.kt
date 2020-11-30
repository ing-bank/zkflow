package com.ing.zknotary.common.contracts
import com.ing.zknotary.annotations.ZKPState
import com.ing.zknotary.common.dactyloscopy.NonFingerprintable
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.zkp.CircuitMetaData
import com.ing.zknotary.common.zkp.ZKNulls
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import java.io.File
import java.util.Random

class TestContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.ing.zknotary.common.contracts.TestContract"
    }

    @BelongsToContract(TestContract::class)
    @ZKPState
    data class TestState(
        override val owner: AbstractParty,
        val value: Int = Random().nextInt(1000)
    ) : ContractState, OwnableState {

        @NonFingerprintable("Temporary removed from fingerprinting")
        override val participants = listOf(owner)

        override fun withNewOwner(newOwner: AbstractParty) = CommandAndState(Move(), copy(owner = newOwner))
    }

    // Commands
    class Create : ZKCommandData {
        override val id: Int = 0

        override val circuit: CircuitMetaData =
            CircuitMetaData(folder = File("${System.getProperty("user.dir")}/../prover/circuits/create"))

        override val paddingConfiguration: ComponentPaddingConfiguration
            get() {
                val emptyState = TestState(ZKNulls.NULL_PARTY, 0)
                val transactionStateFiller = ComponentPaddingConfiguration.Filler.TransactionState(
                    TransactionState(emptyState, notary = ZKNulls.NULL_PARTY)
                )
                val stateAndRefFiller = ComponentPaddingConfiguration.Filler.StateAndRef(
                    StateAndRef(
                        TransactionState(emptyState, notary = ZKNulls.NULL_PARTY),
                        StateRef(BLAKE2s256DigestService.zeroHash, 0)
                    )
                )
                val pubKeyFiller = ComponentPaddingConfiguration.Filler.PublicKey(ZKNulls.NULL_PUBLIC_KEY)

                return ComponentPaddingConfiguration.Builder()
                    .inputs(0, stateAndRefFiller)
                    .outputs(2, transactionStateFiller)
                    .references(2, stateAndRefFiller)
                    .attachments(2, ComponentPaddingConfiguration.Filler.SecureHash(SecureHash.zeroHash))
                    .signers(2, pubKeyFiller)
                    .build()
            }
    }

    class Move : ZKCommandData {
        override val id: Int = 1
        override val circuit: CircuitMetaData =
            CircuitMetaData(folder = File("${System.getProperty("user.dir")}/../prover/circuits/move"))

        override val paddingConfiguration: ComponentPaddingConfiguration
            get() {
                val emptyState = TestState(ZKNulls.NULL_PARTY, 0)
                val transactionStateFiller = ComponentPaddingConfiguration.Filler.TransactionState(
                    TransactionState(emptyState, notary = ZKNulls.NULL_PARTY)
                )
                val stateAndRefFiller = ComponentPaddingConfiguration.Filler.StateAndRef(
                    StateAndRef(
                        TransactionState(emptyState, notary = ZKNulls.NULL_PARTY),
                        StateRef(BLAKE2s256DigestService.zeroHash, 0)
                    )
                )

                val pubKeyFiller = ComponentPaddingConfiguration.Filler.PublicKey(ZKNulls.NULL_PUBLIC_KEY)

                return ComponentPaddingConfiguration.Builder()
                    .inputs(2, stateAndRefFiller)
                    .outputs(2, transactionStateFiller)
                    .references(2, stateAndRefFiller)
                    .attachments(2, ComponentPaddingConfiguration.Filler.SecureHash(SecureHash.zeroHash))
                    .signers(2, pubKeyFiller)
                    .build()
            }
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
