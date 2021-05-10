package io.ivno.collateraltoken.workflow

import co.paralleluniverse.fibers.Suspendable
import io.dasl.workflows.api.flows.account.ListAllAccountsFlow
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.initiateFlows
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.utilities.unwrap

/**
 * Represents a flow to fetch AccountState transactions from other counter-parties.
 *
 * @property sessions The observer flow sessions that the signed transaction will be fetched from.
 * @property progressTracker The progress tracker which tracks the progress of this flow.
 */
class FetchAccountsFlow(
    private val sessions: Set<FlowSession>,
    override val progressTracker: ProgressTracker = tracker()
) : FlowLogic<Unit>() {

    companion object {
        @JvmStatic
        fun tracker() = ProgressTracker(RECEIVING)

        private object RECEIVING : Step("Receiving account transaction.")

        private const val FLOW_VERSION_1 = 1
    }

    @Suspendable
    override fun call() {
        currentStep(RECEIVING)
        sessions.forEach { session ->
            val numberOfTransactions = session.receive<Int>().unwrap { it }
            (1..numberOfTransactions).forEach { _ ->
                subFlow(ReceiveTransactionFlow(session, statesToRecord = StatesToRecord.ALL_VISIBLE))
            }
        }
    }

    /**
     * Represents the initiating flow to fetch AccountState transactions from other parties.
     *
     * @property counterparties The parties who the transaction will be fetched from.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val counterparties: Set<Party>
    ) : FlowLogic<Unit>() {

        private companion object {
            object FETCHING : Step("Fetching accounts from counter-parties.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(FETCHING)

        @Suspendable
        override fun call() {
            currentStep(FETCHING)
            subFlow(FetchAccountsFlow(initiateFlows(counterparties), FETCHING.childProgressTracker()))
        }
    }

    /**
     * Represents the observing flow which is initiated by the [Initiator] flow when
     * fetching AccountState transactions from other parties.
     *
     * @property session The flow session with the initiating counter-party.
     */
    @InitiatedBy(Initiator::class)
    class Observer(private val session: FlowSession) : FlowLogic<Unit>() {

        private companion object {
            object SENDING : Step("Sending transactions.")
        }

        override val progressTracker: ProgressTracker = ProgressTracker(SENDING)

        @Suspendable
        override fun call() {
            currentStep(SENDING)
            val accounts = subFlow(ListAllAccountsFlow(owningParty = ourIdentity))
            val transactions = accounts.map {
                serviceHub.validatedTransactions.getTransaction(it.ref.txhash)
                    ?: throw FlowException("Failed to find transaction with id: ${it.ref.txhash}.")
            }.toSet()
            session.send(transactions.size)
            if (transactions.isNotEmpty()) {
                transactions.forEach { transaction -> subFlow(SendTransactionFlow(session, transaction)) }
            }
        }
    }
}
