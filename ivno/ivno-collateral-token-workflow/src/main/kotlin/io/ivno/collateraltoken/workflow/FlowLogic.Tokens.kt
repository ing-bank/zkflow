package io.ivno.collateraltoken.workflow

import co.paralleluniverse.fibers.Suspendable
import io.dasl.workflows.api.flows.token.flows.MultiAccountTokenTransferRecipientFlow
import io.dasl.workflows.api.flows.token.flows.functions.TransferTokenSenderFunctions.collectTokenMoveSignatures
import io.ivno.collateraltoken.contract.Transfer
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap

@Suspendable
fun FlowLogic<*>.collectTokenMoveSignatures(
    transaction: SignedTransaction,
    transfer: Transfer,
    sessions: Set<FlowSession>
): SignedTransaction {
    val signingSessions = sessions.filter { it.counterparty == transfer.getRequiredCounterparty() }
    sessions.forEach { it.send(it in signingSessions) }
    return collectTokenMoveSignatures(transaction, signingSessions)
}

@Suspendable
fun FlowLogic<*>.collectTokenMoveSignaturesHandler(session: FlowSession) {
    if (session.receive<Boolean>().unwrap { it }) {
        subFlow(MultiAccountTokenTransferRecipientFlow(session))
    }
}
