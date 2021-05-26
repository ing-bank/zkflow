package io.ivno.collateraltoken.workflow.token

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.workflow.*
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.getPreferredNotary
import io.onixlabs.corda.core.workflow.initiateFlows
import io.onixlabs.corda.identityframework.workflow.INITIALIZING
import io.onixlabs.corda.identityframework.workflow.checkHasSufficientFlowSessions
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

/**
 * Represents a flow for creating an [IvnoTokenType] state.
 *
 * @property tokenType The [IvnoTokenType] to create.
 * @property notary The notary which will be assigned to the transaction.
 * @property sessions The required (participant) and optional (observer) flow sessions.
 * @property progressTracker The progress tracker which tracks the progress of this flow.
 */
class CreateIvnoTokenTypeFlow(
    private val tokenType: IvnoTokenType,
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
        checkHasSufficientFlowSessions(sessions, tokenType)

        val (membership, attestation) = resolveOurMembershipAndAttestation(
            tokenType.network,
            tokenType.tokenIssuingEntity
        )

        val unsignedTransaction = transaction(notary) {
            addMembershipReferences(membership.referenced(), attestation.referenced())
            addIvnoTokenTypeCreation(tokenType)
        }

        val signedTransaction = verifyAndSign(unsignedTransaction, tokenType.issuer.owningKey)
        return finalize(signedTransaction, sessions)
    }

    /**
     * Represents the initiating flow for creating an [IvnoTokenType] state.
     *
     * @property tokenType The [IvnoTokenType] to create.
     * @property notary The notary which will be assigned to the transaction.
     * @property observers Optional observers of the token type creation transaction.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val tokenType: IvnoTokenType,
        private val notary: Party? = null,
        private val observers: Set<Party> = emptySet()
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object CREATING : Step("Creating token type.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(CREATING)

        @Suspendable
        override fun call(): SignedTransaction {
            currentStep(CREATING)
            return subFlow(
                CreateIvnoTokenTypeFlow(
                    tokenType,
                    notary ?: getPreferredNotary(),
                    initiateFlows(observers, tokenType),
                    CREATING.childProgressTracker()
                )
            )
        }
    }
}
