package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.client.flows.ZKSignTransactionFlow
import com.ing.zknotary.client.flows.signInitialZKTransaction
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import com.ing.zknotary.testing.fixtures.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
class TestCollectZKSignaturesFlow(val signers: List<Party> = emptyList()) : FlowLogic<SignedZKVerifierTransaction>() {

    @Suspendable
    override fun call(): SignedZKVerifierTransaction {

        val zkService = serviceHub.getCordaServiceFromConfig<ZKTransactionService>(ServiceNames.ZK_TX_SERVICE)
        val me = serviceHub.myInfo.legalIdentities.single()
        val state = TestContract.TestState(me)
        val issueCommand = Command(TestContract.Create(), (signers + me).map { it.owningKey }) //
        val stateAndContract = StateAndContract(state, TestContract.PROGRAM_ID)

        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(stateAndContract, issueCommand)
        val ltx = builder.toWireTransaction(serviceHub).toLedgerTransaction(serviceHub)
        ltx.verify()

        // Sign plaintext transaction
        val stx = serviceHub.signInitialTransaction(builder)

        val vtx = zkService.prove(stx.tx, ltx.inputs, ltx.references)

        // Sign ZKP transaction
        val svtx = signInitialZKTransaction(vtx)

        // Sign with counterparty
        return subFlow(ZKCollectSignaturesFlow(stx, svtx, signers.map { initiateFlow(it) }))
    }
}

@InitiatedBy(TestCollectZKSignaturesFlow::class)
class CounterpartySigner(val otherPartySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val flow = object : ZKSignTransactionFlow(otherPartySession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // In non-test scenario here counterparty should verify incoming Tx including ZK Merkle tree calculation
            }
        }

        // Invoke the subFlow, in response to the counterparty calling [ZKCollectSignaturesFlow].
        subFlow(flow)
    }
}
