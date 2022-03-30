package com.ing.zkflow.integration.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.client.flows.ZKCollectSignaturesFlow
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.client.flows.ZKReceiveFinalityFlow
import com.ing.zkflow.client.flows.ZKReceiveStateAndRefFlow
import com.ing.zkflow.client.flows.ZKSendStateAndRefFlow
import com.ing.zkflow.client.flows.ZKSignTransactionFlow
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import com.ing.zkflow.common.transactions.zkVerify
import com.ing.zkflow.integration.contract.TestContract
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap

/**
 * Disclaimer: this is not how it is supposed to be used in "real" flows, it works just for this test
 */
@InitiatingFlow
class MoveBidirectionalFlow(
    private val myInput: StateAndRef<TestContract.TestState>,
    private val counterParty: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val session = initiateFlow(counterParty)

        // Send a proposed state to trade to counterparty
        session.send(myInput)

        // Receive a state of the same value in return.
        val theirInput = subFlow<List<StateAndRef<TestContract.TestState>>>(ZKReceiveStateAndRefFlow(session)).single()

        // Now we create the transaction
        val me = serviceHub.myInfo.legalIdentities.single().anonymise()
        val command = Command(TestContract.MoveBidirectional(), listOf(counterParty, me).map { it.owningKey })
        val myOutput = StateAndContract(myInput.state.data.copy(owner = counterParty.anonymise()), TestContract.PROGRAM_ID)
        val theirOutput = StateAndContract(theirInput.state.data.copy(owner = me), TestContract.PROGRAM_ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single()).withItems(myInput, theirInput, myOutput, theirOutput, command)
        builder.enforcePrivateInputsAndReferences(serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE))

        // Transaction creator signs transaction.
        val stx = serviceHub.signInitialTransaction(builder)

        // Verify to be sure
        stx.zkVerify(serviceHub, checkSufficientSignatures = false)

        // Collect signature from counterparty
        val fullySignedStx = subFlow(ZKCollectSignaturesFlow(stx, listOf(session)))

        // Finalize
        return subFlow(ZKFinalityFlow(fullySignedStx, listOf(session)))
    }

    companion object {
        @InitiatedBy(MoveBidirectionalFlow::class)
        class Verifier(val session: FlowSession) : FlowLogic<Unit>() {
            @Suspendable
            override fun call() {
                class SignFlow(val myState: StateAndRef<ContractState>) : ZKSignTransactionFlow(session) {
                    @Suspendable
                    override fun checkTransaction(stx: SignedTransaction) = requireThat {
                        "The output I proposed is an input" using (stx.inputs.contains(myState.ref))
                    }
                }
                val initiatorState = session.receive<StateAndRef<TestContract.TestState>>().unwrap { it }

                // Create a state of the same value as the one proposed by initiator
                val createStx = subFlow(CreateFlow(initiatorState.state.data.value))

                // Send it back.
                val output = createStx.tx.outRef<TestContract.TestState>(0)
                subFlow(ZKSendStateAndRefFlow(session, listOf(output)))

                // Invoke the signing subFlow, in response to the counterparty calling [ZKCollectSignaturesFlow].
                val stx = subFlow(SignFlow(output))

                // Invoke flow in response to ZKFinalityFlow
                subFlow(ZKReceiveFinalityFlow(session, stx.id))
            }
        }
    }
}
