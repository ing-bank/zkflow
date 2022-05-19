package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.CBDCContract
import com.example.token.cbdc.CBDCToken
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

@InitiatingFlow
class IssuePrivateCBDCTokenFlow(
    private val token: CBDCToken,
    ) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val issueCommand = Command(CBDCContract.IssuePrivate(), token.issuer.owningKey) //
        val stateAndContract = StateAndContract(token, CBDCContract.ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
            .withItems(stateAndContract, issueCommand)

        val stx = serviceHub.signInitialTransaction(builder)

        subFlow(ZKFinalityFlow(stx, listOf(initiateFlow(token.holder))))

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
