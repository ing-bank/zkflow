package com.ing.zkflow.integration.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.client.flows.ZKCollectSignaturesFlow
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.client.flows.ZKReceiveFinalityFlow
import com.ing.zkflow.client.flows.ZKSignTransactionFlow
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import com.ing.zkflow.integration.contract.TestContract
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.getCordaServiceFromConfig
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
    private val newOwner: Party,
    private val moveCommand: ZKCommandData = TestContract.Move()
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val session = initiateFlow(newOwner)

        val me = serviceHub.myInfo.legalIdentities.single()
        val stateAndRef = createStx.coreTransaction.outRef<TestContract.TestState>(0)
        val command = Command(moveCommand, listOf(newOwner, me).map { it.owningKey })
        val stateAndContract = StateAndContract(stateAndRef.state.data.copy(owner = newOwner.anonymise()), TestContract.PROGRAM_ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(stateAndRef, stateAndContract, command)
        builder.enforcePrivateInputsAndReferences(serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE))

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
