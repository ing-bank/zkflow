package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.MockAssetContract
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
class IssuePrivateFlow(private val value: Int? = null) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val me = serviceHub.myInfo.legalIdentities.single().anonymise()
        val state = if (value != null) MockAssetContract.MockAsset(me, value) else MockAssetContract.MockAsset(me)
        val issueCommand = Command(MockAssetContract.Issue(), me.owningKey) //
        val stateAndContract = StateAndContract(state, MockAssetContract.ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
            .withItems(stateAndContract, issueCommand)
        // builder.enforcePrivateInputsAndReferences(serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE))

        val stx = serviceHub.signInitialTransaction(builder)
        // val fullySignedStx = subFlow(ZKCollectSignaturesFlow(stx, emptyList()))

        subFlow(ZKFinalityFlow(stx, listOf()))

        return stx
    }
}
