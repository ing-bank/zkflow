package io.ivno.collateraltoken.workflow

import co.paralleluniverse.fibers.Suspendable
import io.dasl.workflows.api.flows.account.GetAccountFlow
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.findTransaction
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.utilities.unwrap

/**
 * Represents a flow to fetch an AccountState from other counter-parties.
 *
 * @property session The observer flow session that the AccountState will be fetched from.
 * @property accountId The accountId of the AccountState to fetch.
 * @property progressTracker The progress tracker which tracks the progress of this flow.
 */
class FetchAccountFlow(
    private val session: FlowSession,
    private val accountId: String,
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
        session.sendAndReceive<Any>(accountId).unwrap {
            if (it is SignedTransaction) {
                serviceHub.recordTransactions(StatesToRecord.ALL_VISIBLE, listOf(it))
            }
        }
    }

    /**
     * Represents the initiating flow to fetch an AccountState from other parties.
     *
     * @property counterparty The party who the transaction will be fetched from.
     * @property accountId The accountId of the AccountState to fetch.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val counterparty: Party,
        private val accountId: String
    ) : FlowLogic<Unit>() {

        private companion object {
            object FETCHING : Step("Fetching account from counter-party.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(FETCHING)

        @Suspendable
        override fun call() {
            currentStep(FETCHING)
            subFlow(FetchAccountFlow(initiateFlow(counterparty), accountId, FETCHING.childProgressTracker()))
        }
    }

    /**
     * Represents the observing flow which is initiated by the [Initiator] flow when
     * fetching an AccountState from other parties.
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
            val accountId = session.receive<String>().unwrap { it }
            val tx = findTransaction(subFlow(GetAccountFlow(accountId)))
            session.send(tx)
        }
    }
}
