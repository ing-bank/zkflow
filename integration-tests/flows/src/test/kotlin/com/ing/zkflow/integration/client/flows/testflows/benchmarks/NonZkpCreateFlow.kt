package com.ing.zkflow.integration.client.flows.testflows.benchmarks

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.integration.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
class NonZkpCreateFlow : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val me = serviceHub.myInfo.legalIdentities.single().anonymise()
        val state = TestContract.TestState(me)
        val issueCommand = Command(TestContract.Create(), me.owningKey) //
        val stateAndContract = StateAndContract(state, TestContract.PROGRAM_ID)

        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(stateAndContract, issueCommand)

        val stx = subFlow(CollectSignaturesFlow(serviceHub.signInitialTransaction(builder), emptyList()))

        subFlow(FinalityFlow(stx, listOf()))

        return stx
    }
}
