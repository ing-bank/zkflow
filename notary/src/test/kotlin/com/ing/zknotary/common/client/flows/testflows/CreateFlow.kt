package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.client.flows.ZKFinalityFlow
import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import com.ing.zknotary.common.transactions.signInitialTransaction
import com.ing.zknotary.testing.fixtures.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
class CreateFlow(private val value: Int? = null) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val me = serviceHub.myInfo.legalIdentities.single().anonymise()
        val state = if (value != null) TestContract.TestState(me, value) else TestContract.TestState(me)
        val issueCommand = Command(TestContract.Create(), me.owningKey) //
        val stateAndContract = StateAndContract(state, TestContract.PROGRAM_ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
            .withItems(stateAndContract, issueCommand)

        val stx = serviceHub.signInitialTransaction(builder)
        val fullySignedStx = subFlow(ZKCollectSignaturesFlow(stx, emptyList()))

        subFlow(ZKFinalityFlow(fullySignedStx, listOf()))

        return stx
    }
}
