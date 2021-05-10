package io.ivno.collateraltoken.workflow.token

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.workflow.RECORDING
import io.ivno.collateraltoken.workflow.finalizeHandler
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class CreateIvnoTokenTypeFlowHandler(
    private val session: FlowSession,
    override val progressTracker: ProgressTracker = tracker()
) : FlowLogic<SignedTransaction>() {

    companion object {
        @JvmStatic
        fun tracker() = ProgressTracker(RECORDING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        return finalizeHandler(session)
    }

    @InitiatedBy(CreateIvnoTokenTypeFlow.Initiator::class)
    private class Handler(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

        private companion object {
            object HANDLING : ProgressTracker.Step("Handling token type creation.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(HANDLING)

        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(CreateIvnoTokenTypeFlowHandler(session, HANDLING.childProgressTracker()))
        }
    }
}
