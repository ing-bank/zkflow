package com.ing.zknotary.testing.fixtures.contract

import com.ing.zknotary.testing.fixtures.state.Deal
import com.ing.zknotary.testing.fixtures.state.DealState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.hours
import net.corda.finance.EUR
import net.corda.finance.GBP
import net.corda.finance.JPY
import net.corda.finance.RUB
import net.corda.finance.USD
import java.time.Instant

public class DealContract : Contract {

    public interface Commands : CommandData {
        public class Propose : TypeOnlyCommandData(), Commands
        public class Accept : TypeOnlyCommandData(), Commands
        public class Expire : TypeOnlyCommandData(), Commands
    }

    private val acceptableCurrencies = listOf(GBP, RUB, USD, EUR, JPY)

    private inline fun <reified T : ContractState> LedgerTransaction.extractByType(): Pair<List<T>, List<T>> =
        this.inputsOfType(T::class.java) to this.outputsOfType(T::class.java)

    override fun verify(tx: LedgerTransaction) {
        tx.commands.map {
            when (it.value) {
                is Commands.Propose -> {
                    val (inputs, outputs) = tx.extractByType<DealState>()

                    requireThat {
                        "there is no input state of type ${DealState::class.simpleName}" using (inputs.isEmpty())
                        "there is one output state of type ${DealState::class.simpleName}" using (outputs.singleOrNull() != null)
                    }

                    val output = outputs.single()

                    requireThat {
                        "Timewindow until time must be before expiryDate" using (output.deal.expiryDate.toInstant() > tx.timeWindow?.untilTime)
                        "Proposer must enter reference" using (output.deal.proposerReference != null)
                        "Status must be ${DealState.Status.PROPOSED}" using (output.status == DealState.Status.PROPOSED)
                        "Amount must be > 0" using (output.deal.amount.quantity > 0L)
                        "Weight must be > 0" using (output.deal.weight.quantity > 0.0)
                        "Amount is an acceptable Currency" using (output.deal.amount.token in acceptableCurrencies)
                    }
                }
                is Commands.Accept -> {
                    val (inputs, outputs) = tx.extractByType<DealState>()

                    requireThat {
                        "there is one input state of type ${DealState::class.simpleName}" using (inputs.singleOrNull() != null)
                        "there is one output state of type ${DealState::class.simpleName}" using (outputs.singleOrNull() != null)
                    }

                    val output = outputs.single()
                    val input = inputs.single()

                    requireThat {
                        "The deal is unchanged" using (input equalsExceptStatusAndChanged output)
                        "Timewindow until time must be before expiryDate" using (output.deal.expiryDate.toInstant() > tx.timeWindow?.untilTime)
                        "Status must be ${DealState.Status.ACCEPTED}" using (output.status == DealState.Status.ACCEPTED)
                    }
                }
                is Commands.Expire -> {
                    val (inputs, outputs) = tx.extractByType<DealState>()

                    requireThat {
                        "there is one input state of type ${DealState::class.simpleName}" using (inputs.singleOrNull() != null)
                        "there is no output state of type ${DealState::class.simpleName}" using (outputs.isEmpty())
                    }

                    val output = outputs.single()
                    val input = inputs.single()

                    requireThat {
                        "The deal is unchanged" using (input equalsExceptStatusAndChanged output)
                        "Timewindow until time must be after or on expiryDate" using (output.deal.expiryDate.toInstant() <= tx.timeWindow?.untilTime)
                        "Status must be ${DealState.Status.EXPIRED}" using (output.status == DealState.Status.EXPIRED)
                    }
                }
                else -> error("Unknown command ${it.value::class.simpleName}")
            }
        }
    }

    public fun generateProposal(deal: Deal, notary: Party): TransactionBuilder =
        TransactionBuilder(notary).apply {
            val output = DealState(deal = deal)
            setTimeWindow(Instant.now(), 2.hours)
            addCommand(Commands.Propose(), deal.proposer.owningKey)
            addOutputState(output)
        }

    public fun generateAcceptance(dealStateAndRef: StateAndRef<DealState>): TransactionBuilder =
        TransactionBuilder(dealStateAndRef.state.notary).apply {
            val dealState = dealStateAndRef.state.data
            setTimeWindow(Instant.now(), 2.hours)
            addInputState(dealStateAndRef)
            addCommand(Commands.Accept(), dealState.deal.proposer.owningKey)
            addOutputState(dealState.accept())
        }

    public fun generateExpiry(dealStateAndRef: StateAndRef<DealState>): TransactionBuilder =
        TransactionBuilder(dealStateAndRef.state.notary).apply {
            val dealState = dealStateAndRef.state.data
            setTimeWindow(Instant.now(), 2.hours)
            addInputState(dealStateAndRef)
            addCommand(Commands.Expire(), dealState.deal.proposer.owningKey)
            addOutputState(dealState.expire())
        }
}