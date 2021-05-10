package io.ivno.collateraltoken.workflow.deposit

import co.paralleluniverse.fibers.Suspendable
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.workflow.*
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.initiateFlows
import io.onixlabs.corda.identityframework.workflow.checkHasSufficientFlowSessions
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class RequestDepositFlow(
    private val deposit: Deposit,
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
        checkHasSufficientFlowSessions(sessions, deposit)

        val tokenType = deposit.amount.amountType.resolve(serviceHub)
        val membershipReferences = synchronizeMembership(tokenType.state.data.network, deposit, sessions)

        val unsignedTransaction = transaction(notary) {
            addMembershipReferences(membershipReferences)
            addDepositRequest(deposit)
        }

        val signedTransaction = verifyAndSign(unsignedTransaction, deposit.depositor.owningKey)
        return finalize(signedTransaction, sessions)
    }

    @StartableByRPC
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val deposit: Deposit,
        private val notary: Party? = null,
        private val observers: Set<Party> = emptySet()
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object REQUESTING : ProgressTracker.Step("Requesting deposit.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(REQUESTING)

        @Suspendable
        override fun call(): SignedTransaction {
            currentStep(REQUESTING)
            return subFlow(
                RequestDepositFlow(
                    deposit,
                    notary ?: getPreferredNotary(),
                    initiateFlows(observers, deposit),
                    REQUESTING.childProgressTracker()
                )
            )
        }
    }
}
