package com.ing.zkflow.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.client.flows.ZKCollectSignaturesFlow
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.client.flows.ZKReceiveFinalityFlow
import com.ing.zkflow.client.flows.ZKSignTransactionFlow
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import com.ing.zkflow.testing.fixtures.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
class MoveFlow(
    private val createStx: SignedTransaction,
    private val newOwner: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val session = initiateFlow(newOwner)

        val me = serviceHub.myInfo.legalIdentities.single()
        val stateAndRef = createStx.coreTransaction.outRef<TestContract.TestState>(0)
        val command = Command(TestContract.Move(), listOf(newOwner, me).map { it.owningKey })
        val stateAndContract = StateAndContract(stateAndRef.state.data.copy(owner = newOwner.anonymise()), TestContract.PROGRAM_ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(stateAndRef, stateAndContract, command)

        // Transaction creator signs transaction.
        val selfSignedStx = serviceHub.signInitialTransaction(builder)
        selfSignedStx.verify(serviceHub, false)

        val stx = subFlow(ZKCollectSignaturesFlow(selfSignedStx, listOf(session)))

        subFlow(ZKFinalityFlow(stx, listOf(session)))

        return stx
    }

    companion object {

        @InitiatedBy(MoveFlow::class)
        class Verifier(val session: FlowSession) : FlowLogic<Unit>() {

            @Suspendable
            override fun call() {
                val flow = object : ZKSignTransactionFlow(session) {
                    @Suspendable
                    override fun checkTransaction(stx: SignedTransaction) = requireThat {
                        // In non-test scenario here counterparty can verify incoming Tx from business perspective
                    }
                }

                // Invoke the subFlow, in response to the counterparty calling [ZKCollectSignaturesFlow].
                val stx = subFlow(flow)

                // Invoke flow in response to ZKFinalityFlow
                subFlow(ZKReceiveFinalityFlow(session, stx.id))
            }
        }
    }
}
