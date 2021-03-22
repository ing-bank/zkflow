package com.ing.zknotary.common.client.flows.testflows.benchmarks

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.testing.fixtures.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Disclaimer: this is not how it is supposed to be used in "real" flows, it works just for this test
 */
@InitiatingFlow
class NonZkpMoveFlow(
    private val createStx: SignedTransaction,
    private val newOwner: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val session = initiateFlow(newOwner)

        val me = serviceHub.myInfo.legalIdentities.single()
        val state = createStx.coreTransaction.outRef<TestContract.TestState>(0)
        val command = Command(TestContract.Move(), listOf(newOwner, me).map { it.owningKey })
        val stateAndContract = StateAndContract(state.state.data.copy(owner = newOwner), TestContract.PROGRAM_ID)

        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(state, stateAndContract, command)

        // Transaction creator signs transaction.
        val stx = serviceHub.signInitialTransaction(builder)
        stx.verify(serviceHub, false)

        val sstx = subFlow(CollectSignaturesFlow(stx, listOf(session)))

        subFlow(FinalityFlow(sstx, listOf(session)))

        return sstx
    }

    companion object {

        @InitiatedBy(NonZkpMoveFlow::class)
        class Verifier(val session: FlowSession) : FlowLogic<Unit>() {

            @Suspendable
            override fun call() {
                val flow = object : SignTransactionFlow(session) {
                    @Suspendable
                    override fun checkTransaction(stx: SignedTransaction) = requireThat {
                        // In non-test scenario here counterparty can verify incoming Tx from business perspective
                    }
                }

                // Invoke the subFlow, in response to the counterparty calling [CollectSignaturesFlow].
                val stx = subFlow(flow)

                // Invoke flow in response to FinalityFlow
                subFlow(ReceiveFinalityFlow(session, stx.id))
            }
        }
    }
}
