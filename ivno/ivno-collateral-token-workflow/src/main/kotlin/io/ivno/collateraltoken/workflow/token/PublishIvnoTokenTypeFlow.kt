package io.ivno.collateraltoken.workflow.token

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.findTransaction
import io.onixlabs.corda.core.workflow.initiateFlows
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

/**
 * Represents a flow to publish [IvnoTokenType] transactions to additional observers.
 *
 * @property transaction The [SignedTransaction] containing the [IvnoTokenType] to be sent to observers.
 * @property sessions The observer flow sessions that the signed transaction will be sent to.
 * @property progressTracker The progress tracker which tracks the progress of this flow.
 */
class PublishIvnoTokenTypeFlow(
    private val transaction: SignedTransaction,
    private val sessions: Set<FlowSession>,
    override val progressTracker: ProgressTracker = tracker()
) : FlowLogic<SignedTransaction>() {

    companion object {
        @JvmStatic
        fun tracker() = ProgressTracker(SENDING)

        private object SENDING : Step("Sending transaction.")

        private const val FLOW_VERSION_1 = 1
    }

    @Suspendable
    override fun call(): SignedTransaction {
        currentStep(SENDING)
        sessions.forEach { subFlow(SendTransactionFlow(it, transaction)) }
        return transaction
    }

    /**
     * Represents the initiating flow to publish [IvnoTokenType] transactions to additional observers.
     *
     * @property tokenTypeLinearId The ID of the token type which will be used to find the relevant transaction.
     * @property observers The observers who the transaction will be sent to.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val tokenTypeLinearId: UniqueIdentifier,
        private val observers: Set<Party>
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object PUBLISHING : Step("Publishing token type.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(PUBLISHING)

        @Suspendable
        override fun call(): SignedTransaction {
            currentStep(PUBLISHING)
            return subFlow(
                PublishIvnoTokenTypeFlow(
                    findTransaction(subFlow(FindIvnoTokenTypeFlow(linearId = tokenTypeLinearId))!!),
                    initiateFlows(observers),
                    PUBLISHING.childProgressTracker()
                )
            )
        }
    }
}
