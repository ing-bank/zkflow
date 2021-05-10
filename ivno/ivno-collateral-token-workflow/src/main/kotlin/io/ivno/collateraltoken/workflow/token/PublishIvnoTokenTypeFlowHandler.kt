package io.ivno.collateraltoken.workflow.token

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ReceiveTransactionFlow
import net.corda.core.node.StatesToRecord
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

class PublishIvnoTokenTypeFlowHandler(
    private val session: FlowSession,
    override val progressTracker: ProgressTracker = tracker()
) : FlowLogic<Unit>() {

    companion object {
        object RECORDING : Step("Recording transaction.")

        @JvmStatic
        fun tracker() = ProgressTracker(RECORDING)
    }

    @Suspendable
    override fun call() {
        subFlow(ReceiveTransactionFlow(session, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }

    @InitiatedBy(PublishIvnoTokenTypeFlow.Initiator::class)
    private class Handler(private val session: FlowSession) : FlowLogic<Unit>() {

        private companion object {
            object HANDLING : Step("Handling token type publication.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(HANDLING)

        @Suspendable
        override fun call() {
            return subFlow(PublishIvnoTokenTypeFlowHandler(session, HANDLING.childProgressTracker()))
        }
    }
}
