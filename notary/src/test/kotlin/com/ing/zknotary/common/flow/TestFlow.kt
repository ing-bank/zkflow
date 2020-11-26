package com.ing.zknotary.common.flow

import com.ing.zknotary.client.flows.TransactionsPair
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.common.contracts.TestContract
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.TransactionBuilder


class TestCreateFlow(val value: String) : FlowLogic<TransactionsPair>() {

    override fun call(): TransactionsPair {

        val state = TestContract.TestState(serviceHub.myInfo.legalIdentities.single())
        val issueCommand = TestContract.Create()

        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(state, issueCommand)
        builder.toWireTransaction(serviceHub).toLedgerTransaction(serviceHub).verify()

        // Transaction creator signs transaction.
        val ptx = serviceHub.signInitialTransaction(builder)
        val pztx = signInitialZKTransaction(ptx)

        return subFlow(ZKCollectSignaturesFlow(ptx, pztx, emptyList()))
    }

}
