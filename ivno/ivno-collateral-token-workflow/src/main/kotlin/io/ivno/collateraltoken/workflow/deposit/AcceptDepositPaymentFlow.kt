package io.ivno.collateraltoken.workflow.deposit

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.workflow.*
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.initiateFlows
import io.onixlabs.corda.identityframework.workflow.checkHasSufficientFlowSessions
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class AcceptDepositPaymentFlow(
    private val oldDeposit: StateAndRef<Deposit>,
    private val newDeposit: Deposit,
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
        checkHasSufficientFlowSessions(sessions, newDeposit)

        val tokenType = newDeposit.amount.amountType.resolve(serviceHub)
        val membershipReferences = synchronizeMembership(tokenType.state.data.network, newDeposit, sessions)
        val account = resolveAccount(newDeposit.accountId, newDeposit.depositor, sessions).state.data

        val unsignedTransaction = transaction(oldDeposit.state.notary) {
            addMembershipReferences(membershipReferences)
            addIssuedToken(
                newDeposit.amount,
                account,
                tokenType,
                newDeposit.depositor,
                newDeposit.custodian,
                serviceHub
            )
            addDepositAdvance(oldDeposit, newDeposit)
        }

        val partiallySignedTransaction = verifyAndSign(unsignedTransaction, ourIdentity.owningKey)

        val signingSessions = notifyAndGetSigningSessions(sessions, newDeposit.getRequiredSigningKeys())
        val fullySignedTransaction = countersign(partiallySignedTransaction, signingSessions)
        return finalize(fullySignedTransaction, sessions)
    }

    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val oldDeposit: StateAndRef<Deposit>,
        private val newDeposit: Deposit,
        private val observers: Set<Party> = emptySet()
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object ACCEPTING : ProgressTracker.Step("Accepting deposit payment.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(ACCEPTING)

        @Suspendable
        override fun call(): SignedTransaction {
            currentStep(ACCEPTING)
            return subFlow(
                AcceptDepositPaymentFlow(
                    oldDeposit,
                    newDeposit,
                    initiateFlows(observers, newDeposit),
                    ACCEPTING.childProgressTracker()
                )
            )
        }
    }
}
