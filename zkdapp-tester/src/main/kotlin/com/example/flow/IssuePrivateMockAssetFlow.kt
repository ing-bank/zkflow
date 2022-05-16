package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.MockAssetContract
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
class IssuePrivateMockAssetFlow(private val value: Int? = null) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val me = serviceHub.myInfo.legalIdentities.single().anonymise()
        val state = if (value != null) MockAssetContract.MockAsset(me, value) else MockAssetContract.MockAsset(me)
        val issueCommand = Command(MockAssetContract.IssuePrivate(), me.owningKey) //
        val stateAndContract = StateAndContract(state, MockAssetContract.ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
            .withItems(stateAndContract, issueCommand)

        val stx = serviceHub.signInitialTransaction(builder)

        subFlow(ZKFinalityFlow(stx, listOf()))

        return stx
    }
}
