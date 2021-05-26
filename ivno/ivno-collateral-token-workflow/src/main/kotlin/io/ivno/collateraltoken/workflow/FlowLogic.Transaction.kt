package io.ivno.collateraltoken.workflow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.participants
import io.onixlabs.corda.core.workflow.currentStep
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.security.PublicKey

/**
 * Generates an unsigned transaction.
 *
 * @param notary The notary to use for the generated transaction.
 * @param action An action which builds and results in the unsigned transaction.
 * @return Returns a [TransactionBuilder] representing the unsigned transaction.
 */
@Suspendable
fun FlowLogic<*>.transaction(notary: Party, action: TransactionBuilder.() -> TransactionBuilder): TransactionBuilder {
    currentStep(GENERATING)
    return with(TransactionBuilder(notary)) { action(this) }
}

/**
 * Verifies and signs an unsigned transaction.
 *
 * @param builder The unsigned transaction to verify and sign.
 * @param signingKey The required key to sign the transaction.
 * @return Returns a signed transaction.
 */
@Suspendable
fun FlowLogic<*>.verifyAndSign(
    builder: TransactionBuilder,
    signingKey: PublicKey
): SignedTransaction {
    currentStep(VERIFYING)
    builder.verify(serviceHub)

    currentStep(SIGNING)
    return serviceHub.signInitialTransaction(builder, signingKey)
}

/**
 * Obtains counter-party signatures for a transaction.
 *
 * @param signedTransaction The partially signed transaction to countersign.
 * @param sessions The flow sessions for the required counter-signing parties.
 * @return Returns a signed transaction.
 */
@Suspendable
fun FlowLogic<*>.countersign(
    signedTransaction: SignedTransaction,
    sessions: Set<FlowSession>
): SignedTransaction {
    return if (sessions.isNotEmpty()) {
        currentStep(COUNTERSIGNING)
        subFlow(CollectSignaturesFlow(signedTransaction, sessions, COUNTERSIGNING.childProgressTracker()))
    } else signedTransaction
}

/**
 * Handles a request to countersign a transaction.
 *
 * @param session The counter-party flow session who is requesting to sign the transaction.
 * @param action An optional action to perform transaction checking.
 */
@Suspendable
fun FlowLogic<*>.countersignHandler(session: FlowSession, action: (SignedTransaction) -> Unit = {}) {
    if (session.receive<Boolean>().unwrap { it }) {
        currentStep(SIGNING)
        subFlow(object : SignTransactionFlow(session, SIGNING.childProgressTracker()) {
            override fun checkTransaction(stx: SignedTransaction) = action(stx)
        })
    }
}

@Suspendable
fun FlowLogic<*>.notifyAndGetSigningSessions(
    sessions: Iterable<FlowSession>,
    signers: Iterable<PublicKey>
): Set<FlowSession> {
    val requiredSigningSessions = sessions.filter {
        if (it.counterparty in serviceHub.myInfo.legalIdentities) {
            throw FlowException("Do not pass flow sessions for the local node.")
        }

        it.counterparty.owningKey in signers
    }

    sessions.forEach { it.send(it in requiredSigningSessions) }

    return requiredSigningSessions.toSet()
}

@Suspendable
fun FlowLogic<*>.finalize(
    transaction: SignedTransaction,
    sessions: Iterable<FlowSession>,
    observerSessions: Iterable<FlowSession> = emptySet()
): SignedTransaction {

    val observingSessions = if (observerSessions.count() > 0) observerSessions else {
        val ledgerTransaction = transaction.toLedgerTransaction(serviceHub, false)
        val participants = (ledgerTransaction.inputStates + ledgerTransaction.outputStates)
            .flatMap { it.participants }.toSet()

        sessions.filter { it.counterparty !in participants }
    }.toSet()

    sessions.forEach { it.send(it in observingSessions) }

    currentStep(FINALIZING)
    return subFlow(FinalityFlow(transaction, sessions.toSet(), FINALIZING.childProgressTracker()))
}

@Suspendable
fun FlowLogic<*>.finalizeHandler(session: FlowSession): SignedTransaction {
    currentStep(RECORDING)
    val isObserver = session.receive<Boolean>().unwrap { it }
    val statesToRecord = if (isObserver) StatesToRecord.ALL_VISIBLE else StatesToRecord.ONLY_RELEVANT
    return subFlow(ReceiveFinalityFlow(session, null, statesToRecord))
}
