package io.ivno.collateraltoken.workflow.transfer

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.workflow.*
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class RequestTransferFlowHandler(
    private val session: FlowSession,
    override val progressTracker: ProgressTracker = tracker()
) : FlowLogic<SignedTransaction>() {

    companion object {
        @JvmStatic
        fun tracker() = ProgressTracker(SYNCHRONIZING, RECORDING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        synchronizeMembershipHandler(session)
        return finalizeHandler(session)
    }

    @InitiatedBy(RequestTransferFlow.Initiator::class)
    private class Handler(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

        private companion object {
            object HANDLING : ProgressTracker.Step("Handling transfer request.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(HANDLING)

        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(RequestTransferFlowHandler(session, HANDLING.childProgressTracker()))
        }
    }
}
