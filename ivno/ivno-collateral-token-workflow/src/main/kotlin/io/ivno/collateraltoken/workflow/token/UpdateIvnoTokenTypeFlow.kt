package io.ivno.collateraltoken.workflow.token

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.workflow.*
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.initiateFlows
import io.onixlabs.corda.identityframework.workflow.INITIALIZING
import io.onixlabs.corda.identityframework.workflow.checkHasSufficientFlowSessions
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

/**
 * Represents a flow for updating an [IvnoTokenType] state.
 *
 * @property tokenType The [IvnoTokenType] to update.
 * @property notary The notary which will be assigned to the transaction.
 * @property sessions The required (participant) and optional (observer) flow sessions.
 * @property progressTracker The progress tracker which tracks the progress of this flow.
 */
class UpdateIvnoTokenTypeFlow(
    private val oldTokenType: StateAndRef<IvnoTokenType>,
    private val newTokenType: IvnoTokenType,
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
        checkHasSufficientFlowSessions(sessions, newTokenType)

        val (membership, attestation) = resolveOurMembershipAndAttestation(
            newTokenType.network,
            newTokenType.tokenIssuingEntity
        )

        val unsignedTransaction = transaction(oldTokenType.state.notary) {
            addMembershipReferences(membership.referenced(), attestation.referenced())
            addIvnoTokenTypeUpdate(oldTokenType, newTokenType)
        }

        val signedTransaction = verifyAndSign(unsignedTransaction, newTokenType.issuer.owningKey)
        return finalize(signedTransaction, sessions)
    }

    /**
     * Represents the initiating flow for updating an [IvnoTokenType] state.
     *
     * @property oldTokenType The [IvnoTokenType] to update.
     * @property newTokenType The updated [IvnoTokenType].
     * @property observers Optional observers of the token type update transaction.
     */
    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val oldTokenType: StateAndRef<IvnoTokenType>,
        private val newTokenType: IvnoTokenType,
        private val observers: Set<Party> = emptySet()
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object UPDATING : Step("Updating token type.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(UPDATING)

        @Suspendable
        override fun call(): SignedTransaction {
            currentStep(UPDATING)
            return subFlow(
                UpdateIvnoTokenTypeFlow(
                    oldTokenType,
                    newTokenType,
                    initiateFlows(observers, newTokenType),
                    UPDATING.childProgressTracker()
                )
            )
        }
    }
}
