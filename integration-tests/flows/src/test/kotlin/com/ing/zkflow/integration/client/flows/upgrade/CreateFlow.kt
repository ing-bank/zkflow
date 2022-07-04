package com.ing.zkflow.integration.client.flows.upgrade

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
class CreateFlow(private val state: VersionedMyState, private val command: ZKCommandData) :
    FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
            .addOutputState(state, MyContract.PROGRAM_ID)
            .addCommand(command, state.participants.map { it.owningKey })

        val stx = serviceHub.signInitialTransaction(builder)

        subFlow(ZKFinalityFlow(stx, privateSessions = listOf()))

        return stx
    }
}
