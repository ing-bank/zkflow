package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.audit.AuditContract
import com.example.contract.cbdc.CBDCContract
import com.example.contract.cbdc.commands.MovePrivate
import com.example.contract.cbdc.CBDCToken
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.client.flows.ZKReceiveFinalityFlow
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import com.ing.zkflow.common.transactions.verification.zkVerify
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.seconds
import java.time.Instant

/**
 * Use this flow to move a [CBDCToken] privately.
 * Only the current holder and the new holder will be aware of the token's existence,
 * and only the new holder will be able to see its private contents in its vault.
 *
 * This flow should be called by the current holder.
 * The token is moved to the holder specified in the output [CBDCToken].
 * The holder will receive the token correctly in their vault if they have registered the [MovePrivateCBDCTokenFlowFlowHandler].
 *
 * Optionally, an auditor can be set on this move transaction.
 * In that case, an [AuditContract.AuditRecord] will be added to the transaction as a publicly visible output.
 * It will contain some metadata about the transaction.
 * This auditor will NOT receive the full private transaction data, but only the [ZKVerifierTransaction] with the publicly
 * visible components of the transaction.
 */
@InitiatingFlow
class MovePrivateCBDCTokenFlow(
    private val token: StateAndRef<CBDCToken>,
    private val newHolder: AnonymousParty,
    private val auditor: Party?
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val inputState = token.state.data

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
            .addInputState(token)
            .addOutputState(inputState.withNewHolder(newHolder), CBDCContract.ID)
            .addCommand(MovePrivate(), inputState.holder.owningKey)
            .setTimeWindow(Instant.now(), 100.seconds)

        val auditorSessions = mutableListOf<FlowSession>()
        if (auditor != null) {
            builder.addOutputState(AuditContract.AuditRecord(auditor, "Signer moved less than 25k"), AuditContract.ID)
            auditorSessions.add(initiateFlow(auditor))
        }

        val stx = serviceHub.signInitialTransaction(builder)

        stx.zkVerify(serviceHub, checkSufficientSignatures = false)

        subFlow(
            ZKFinalityFlow(
                stx,
                // We send the full transaction data, including private data, to the new holder of the token
                privateSessions = listOf(initiateFlow(newHolder)),
                // If any auditor is set, we send the public transaction data, excluding the private data
                publicSessions = auditorSessions
            )
        )

        return stx
    }
}

@InitiatedBy(MovePrivateCBDCTokenFlow::class)
class MovePrivateCBDCTokenFlowFlowHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            subFlow(ZKReceiveFinalityFlow(otherSession))
        }
    }
}