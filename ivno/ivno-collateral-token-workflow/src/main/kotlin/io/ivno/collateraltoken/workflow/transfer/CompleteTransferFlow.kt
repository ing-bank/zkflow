package io.ivno.collateraltoken.workflow.transfer

import co.paralleluniverse.fibers.Suspendable
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.workflows.api.flows.token.flows.MultiAccountTokenTransferFlow
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.workflow.*
import io.onixlabs.corda.core.workflow.checkSufficientSessions
import io.onixlabs.corda.core.workflow.currentStep
import io.onixlabs.corda.core.workflow.initiateFlows
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import java.time.Instant

class CompleteTransferFlow(
    private val oldTransfer: StateAndRef<Transfer>,
    private val newTransfer: Transfer,
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
        checkSufficientSessions(sessions, newTransfer)

        val tokenType = newTransfer.amount.amountType.resolve(serviceHub)
        val transferRequest = createTransferRequest(newTransfer, tokenType.state.data, oldTransfer.state.notary)
        val membershipReferences = synchronizeMembership(tokenType.state.data.network, newTransfer, sessions)

        val unsignedTransaction = transaction(oldTransfer.state.notary) {
            addMembershipReferences(membershipReferences)
            addMovedToken(transferRequest, serviceHub, ourIdentity)
            addTransferAdvance(oldTransfer, newTransfer)
        }

        val partiallySignedTransaction = verifyAndSign(unsignedTransaction, newTransfer.getRequiredSigningKey())
        val fullySignedTransaction = collectTokenMoveSignatures(partiallySignedTransaction, newTransfer, sessions)
        val observerSessions = sessions.filter { it.counterparty != newTransfer.getRequiredCounterparty() }
        return finalize(fullySignedTransaction, sessions, observerSessions)
    }

    @Suspendable
    private fun createTransferRequest(
        transfer: Transfer,
        tokenType: IvnoTokenType,
        notary: Party
    ): MultiAccountTokenTransferFlow.Request {
        val currentTokenHolderAccountAddress = resolveAccount(transfer.currentTokenHolderAccountId).state.data.address
        val targetTokenHolderAccountAddress = resolveAccount(transfer.targetTokenHolderAccountId).state.data.address
        val amount = BigDecimalAmount(transfer.amount.quantity.setScale(tokenType.exponent), tokenType.descriptor)
        return MultiAccountTokenTransferFlow.Request(
            from = listOf(currentTokenHolderAccountAddress to amount),
            to = listOf(targetTokenHolderAccountAddress to amount),
            transactionTime = Instant.now(),
            notary = notary
        )
    }

    @StartableByRPC
    @StartableByService
    @InitiatingFlow(version = FLOW_VERSION_1)
    class Initiator(
        private val oldTransfer: StateAndRef<Transfer>,
        private val newTransfer: Transfer,
        private val observers: Set<Party> = emptySet(),
        private val includeTokenObservers: Boolean = true
    ) : FlowLogic<SignedTransaction>() {

        private companion object {
            object COMPLETING : ProgressTracker.Step("Completing transfer.") {
                override fun childProgressTracker(): ProgressTracker = tracker()
            }
        }

        override val progressTracker: ProgressTracker = ProgressTracker(COMPLETING)

        @Suspendable
        override fun call(): SignedTransaction {

            val tokenObservers = if (includeTokenObservers) {
                val network = newTransfer.amount.amountType.resolve(serviceHub).state.data.network
                val holder = newTransfer.currentTokenHolder
                getTokenObservers(network, holder)
            } else emptySet()

            currentStep(COMPLETING)
            return subFlow(
                CompleteTransferFlow(
                    oldTransfer,
                    newTransfer,
                    initiateFlows(tokenObservers + observers, newTransfer),
                    COMPLETING.childProgressTracker()
                )
            )
        }
    }
}
