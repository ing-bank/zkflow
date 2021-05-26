package io.ivno.collateraltoken.workflow.transfer

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.workflow.*
import io.onixlabs.corda.core.workflow.checkSufficientSessions
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.initiateFlows
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class AcceptTransferFlow(
    private val oldTransfer: StateAndRef<Transfer>,
    private val newTransfer: Transfer,
    private val sessions: Set<FlowSession>,
    override val progressTracker: ProgressTracker = tracker()
) : FlowLogic<SignedTransaction>() {

    companion object {
        @JvmStatic
        fun tracker() = ProgressTracker(INITIALIZING, GENERATING, VERIFYING, SIGNING, FINALIZING)

        private const val FLOW_VERSION_1 = 1
    }

    @Suspendable
    override fun call(): SignedTransaction {
        currentStep(INITIALIZING)
        checkSufficientSessions(sessions, newTransfer)

        val tokenType = newTransfer.amount.amountType.resolve(serviceHub)
        val membershipReferences = synchronizeMembership(tokenType.state.data.network, newTransfer, sessions)

        val unsignedTransaction = transaction(oldTransfer.state.notary) {
            addMembershipReferences(membershipReferences)
            addTransferAdvance(oldTransfer, newTransfer)
        }

        val signedTransaction = verifyAndSign(unsignedTransaction, newTransfer.getRequiredSigningKey())
        val observerSessions = sessions.filter { it.counterparty != newTransfer.getRequiredCounterparty() }
        return finalize(signedTransaction, sessions, observerSessions)
    }

    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val oldTransfer: StateAndRef<Transfer>,
        private val newTransfer: Transfer,
        private val observers: Set<Party> = emptySet()
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object ACCEPTING : ProgressTracker.Step("Accepting transfer.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(ACCEPTING)

        @Suspendable
        override fun call(): SignedTransaction {
            currentStep(ACCEPTING)
            return subFlow(
                AcceptTransferFlow(
                    oldTransfer,
                    newTransfer,
                    initiateFlows(observers, newTransfer),
                    ACCEPTING.childProgressTracker()
                )
            )
        }
    }
}
