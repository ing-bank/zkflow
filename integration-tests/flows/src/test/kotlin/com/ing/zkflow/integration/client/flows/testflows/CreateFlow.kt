package com.ing.zkflow.integration.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.client.flows.ZKCollectSignaturesFlow
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import com.ing.zkflow.integration.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
class CreateFlow(private val value: Int? = null, private val createCommand: ZKCommandData = TestContract.Create()) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val me = serviceHub.myInfo.legalIdentities.single().anonymise()
        val state = if (value != null) TestContract.TestState(me, value) else TestContract.TestState(me)
        val issueCommand = Command(createCommand, me.owningKey) //
        val stateAndContract = StateAndContract(state, TestContract.PROGRAM_ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
            .withItems(stateAndContract, issueCommand)

        val stx = serviceHub.signInitialTransaction(builder)
        val fullySignedStx = subFlow(ZKCollectSignaturesFlow(stx, emptyList()))

        subFlow(ZKFinalityFlow(fullySignedStx, listOf()))

        return stx
    }
}
