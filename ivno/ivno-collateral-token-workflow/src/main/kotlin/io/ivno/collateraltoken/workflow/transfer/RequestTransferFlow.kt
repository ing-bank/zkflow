package io.ivno.collateraltoken.workflow.transfer

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.workflow.*
import io.onixlabs.corda.core.workflow.checkSufficientSessions
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.initiateFlows
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class RequestTransferFlow(
    private val transfer: Transfer,
    private val notary: Party,
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
        checkSufficientSessions(sessions, transfer)

        val tokenType = transfer.amount.amountType.resolve(serviceHub)
        val membershipReferences = synchronizeMembership(tokenType.state.data.network, transfer, sessions)

        val unsignedTransaction = transaction(notary) {
            addMembershipReferences(membershipReferences)
            addTransferRequest(transfer)
        }

        val signedTransaction = verifyAndSign(unsignedTransaction, transfer.getRequiredSigningKey())
        val observerSessions = sessions.filter { it.counterparty != transfer.getRequiredCounterparty() }
        return finalize(signedTransaction, sessions, observerSessions)
    }

    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val transfer: Transfer,
        private val notary: Party? = null,
        private val observers: Set<Party> = emptySet()
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object REQUESTING : ProgressTracker.Step("Requesting transfer.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(REQUESTING)

        @Suspendable
        override fun call(): SignedTransaction {
            currentStep(REQUESTING)
            return subFlow(
                RequestTransferFlow(
                    transfer,
                    notary ?: getPreferredNotary(),
                    initiateFlows(observers, transfer),
                    REQUESTING.childProgressTracker()
                )
            )
        }
    }
}
