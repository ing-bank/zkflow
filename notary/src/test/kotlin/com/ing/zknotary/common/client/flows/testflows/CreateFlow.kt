package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.client.flows.ZKFinalityFlow
import com.ing.zknotary.client.flows.createSignature
import com.ing.zknotary.client.flows.signInitialZKTransaction
import com.ing.zknotary.common.client.flows.TestSerializationScheme
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import com.ing.zknotary.testing.fixtures.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
class CreateFlow : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val zkService: ZKTransactionService = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)

        val me = serviceHub.myInfo.legalIdentities.single()
        val state = TestContract.TestState(me)
        val issueCommand = Command(TestContract.Create(), me.owningKey) //
        val stateAndContract = StateAndContract(state, TestContract.PROGRAM_ID)

        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(stateAndContract, issueCommand)
        val wtx = builder.toWireTransaction(serviceHub, TestSerializationScheme.SCHEME_ID)

        val vtx = zkService.prove(wtx)

        // We can't use `serviceHub.signInitialTransaction(builder)`,
        // since it prohibits us from setting custom serialization scheme.
        // TODO: We probably have to make that clear to our users, or their code will fail.
        // Probably best to wrap it in a subflow they can call?
        val stx = SignedTransaction(wtx, listOf(serviceHub.createSignature(wtx.id, me.owningKey)))
        val svtx = subFlow(ZKCollectSignaturesFlow(stx, signInitialZKTransaction(vtx), emptyList()))

        subFlow(ZKFinalityFlow(stx, svtx, listOf()))

        return stx
    }
}
