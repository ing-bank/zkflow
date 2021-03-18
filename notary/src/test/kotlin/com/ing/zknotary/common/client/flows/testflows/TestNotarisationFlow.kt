package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.client.flows.ZKNotaryFlow
import com.ing.zknotary.client.flows.signInitialZKTransaction
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import com.ing.zknotary.testing.fixtures.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
class TestNotarisationFlow(val signers: List<Party> = emptyList()) : FlowLogic<SignedZKVerifierTransaction>() {

    @Suspendable
    override fun call(): SignedZKVerifierTransaction {

        val zkService: ZKTransactionService = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)

        val me = serviceHub.myInfo.legalIdentities.single()
        val state = TestContract.TestState(me)
        val issueCommand = Command(TestContract.Create(), (signers + me).map { it.owningKey }) //
        val stateAndContract = StateAndContract(state, TestContract.PROGRAM_ID)

        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(stateAndContract, issueCommand)
        val ltx = builder.toWireTransaction(serviceHub).toLedgerTransaction(serviceHub)
        ltx.verify()

        // Transaction creator signs transaction.
        val stx = serviceHub.signInitialTransaction(builder)

        val vtx = zkService.prove(stx.tx, ltx.inputs, ltx.references)
        val svtx = signInitialZKTransaction(vtx)

        val partiallySignedVTX = subFlow(ZKCollectSignaturesFlow(stx, svtx, signers.map { initiateFlow(it) }))

        val notarySigs = subFlow(ZKNotaryFlow(stx, partiallySignedVTX))

        return svtx + notarySigs
    }
}
