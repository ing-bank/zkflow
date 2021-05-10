package io.ivno.collateraltoken.workflow.redemption

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.contract.Redemption
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

class RejectRedemptionFlow(
    private val oldRedemption: StateAndRef<Redemption>,
    private val newRedemption: Redemption,
    private val sessions: Set<FlowSession>,
    override val progressTracker: ProgressTracker = tracker()
) : FlowLogic<SignedTransaction>() {

    companion object {
        @JvmStatic
        fun tracker() = ProgressTracker(INITIALIZING, GENERATING, VERIFYING, SIGNING, COUNTERSIGNING, FINALIZING)

        private const val FLOW_VERSION_1 = 1
    }

    @Suspendable
    override fun call(): SignedTransaction {
        currentStep(INITIALIZING)
        checkSufficientSessions(sessions, newRedemption)

        val tokenType = newRedemption.amount.amountType.resolve(serviceHub)
        val membershipReferences = synchronizeMembership(tokenType.state.data.network, newRedemption, sessions)
        val account = resolveAccount(newRedemption.accountId, newRedemption.redeemer, sessions).state.data

        val unsignedTransaction = transaction(oldRedemption.state.notary) {
            addMembershipReferences(membershipReferences)
            addIssuedToken(
                newRedemption.amount,
                account,
                tokenType,
                newRedemption.redeemer,
                newRedemption.custodian,
                serviceHub
            )
            addRedemptionAdvance(oldRedemption, newRedemption)
        }

        val partiallySignedTransaction = verifyAndSign(unsignedTransaction, ourIdentity.owningKey)

        val signingSessions = notifyAndGetSigningSessions(sessions, newRedemption.getRequiredSigningKeys())
        val fullySignedTransaction = countersign(partiallySignedTransaction, signingSessions)

        return finalize(fullySignedTransaction, sessions)
    }

    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val oldRedemption: StateAndRef<Redemption>,
        private val newRedemption: Redemption,
        private val observers: Set<Party> = emptySet()
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object REJECTING : ProgressTracker.Step("Rejecting redemption.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(REJECTING)

        @Suspendable
        override fun call(): SignedTransaction {
            currentStep(REJECTING)
            return subFlow(
                RejectRedemptionFlow(
                    oldRedemption,
                    newRedemption,
                    initiateFlows(observers, newRedemption),
                    REJECTING.childProgressTracker()
                )
            )
        }
    }
}
