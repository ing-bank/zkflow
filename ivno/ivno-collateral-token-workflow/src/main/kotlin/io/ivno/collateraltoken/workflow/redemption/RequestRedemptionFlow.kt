package io.ivno.collateraltoken.workflow.redemption

import co.paralleluniverse.fibers.Suspendable
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.workflows.api.flows.token.flows.MultiAccountTokenRedeemFlow
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.workflow.*
import io.onixlabs.corda.core.workflow.checkSufficientSessions
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.initiateFlows
import io.onixlabs.corda.identityframework.workflow.INITIALIZING
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class RequestRedemptionFlow(
    private val redemption: Redemption,
    private val notary: Party,
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
        checkSufficientSessions(sessions, redemption)

        val tokenType = redemption.amount.amountType.resolve(serviceHub)
        val redemptionRequest = createRedemptionRequest(redemption, tokenType.state.data, notary)
        val membershipReferences = synchronizeMembership(tokenType.state.data.network, redemption, sessions)

        val unsignedTransaction = transaction(notary) {
            addMembershipReferences(membershipReferences)
            addRedemptionRequest(redemption)
            addRedeemToken(redemptionRequest, ourIdentity, serviceHub)
        }

        val partiallySignedTransaction = verifyAndSign(unsignedTransaction, ourIdentity.owningKey)

        val signingSessions = notifyAndGetSigningSessions(sessions, redemption.getRequiredSigningKeys())
        val fullySignedTransaction = countersign(partiallySignedTransaction, signingSessions)

        return finalize(fullySignedTransaction, sessions)
    }

    @Suspendable
    private fun createRedemptionRequest(
        redemption: Redemption,
        tokenType: IvnoTokenType,
        notary: Party
    ): MultiAccountTokenRedeemFlow.Request {
        val accountAddress = resolveAccount(redemption.accountId).state.data.address
        val amount = BigDecimalAmount(redemption.amount.quantity.setScale(tokenType.exponent), tokenType.descriptor)
        return MultiAccountTokenRedeemFlow.Request(from = listOf(accountAddress to amount), notary = notary)
    }

    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val redemption: Redemption,
        private val notary: Party? = null,
        private val observers: Set<Party> = emptySet()
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object REQUESTING : ProgressTracker.Step("Requesting redemption.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(REQUESTING)

        @Suspendable
        override fun call(): SignedTransaction {
            currentStep(REQUESTING)
            return subFlow(
                RequestRedemptionFlow(
                    redemption,
                    notary ?: getPreferredNotary(),
                    initiateFlows(observers, redemption),
                    REQUESTING.childProgressTracker()
                )
            )
        }
    }
}
