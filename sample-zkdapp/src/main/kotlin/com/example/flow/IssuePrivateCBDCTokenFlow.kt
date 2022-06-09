package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.cbdc.CBDCContract
import com.example.contract.cbdc.commands.IssuePrivate
import com.example.contract.cbdc.CBDCToken
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.client.flows.ZKReceiveFinalityFlow
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

/**
 * Use this flow to issue a [CBDCToken] privately.
 * Only the issuer and the holder will be aware of the token's existence,
 * and only the holder will be able to see its private contents in its vault.
 *
 * This flow should be called by the issuer.
 * The token is issued to the holder specified in the [CBDCToken].
 * The holder will receive the token correctly in their vault if they have registered the [IssuePrivateCBDCTokenFlowFlowHandler].
 */
@InitiatingFlow
class IssuePrivateCBDCTokenFlow(
    private val token: CBDCToken,
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val issueCommand = Command(IssuePrivate(), token.issuer.owningKey) //
        val stateAndContract = StateAndContract(token, CBDCContract.ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
            .withItems(stateAndContract, issueCommand)

        val stx = serviceHub.signInitialTransaction(builder)

        subFlow(ZKFinalityFlow(stx, privateSessions = listOf(initiateFlow(token.holder)), publicSessions = emptyList()))

        return stx
    }
}

@InitiatedBy(IssuePrivateCBDCTokenFlow::class)
class IssuePrivateCBDCTokenFlowFlowHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            subFlow(ZKReceiveFinalityFlow(otherSession))
        }
    }
}
