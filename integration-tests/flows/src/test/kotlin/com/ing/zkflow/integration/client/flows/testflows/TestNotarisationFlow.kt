package com.ing.zkflow.integration.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.client.flows.ZKCollectSignaturesFlow
import com.ing.zkflow.client.flows.ZKNotaryFlow
import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.signInitialTransaction
import com.ing.zkflow.common.zkp.ZKTransactionService
import com.ing.zkflow.integration.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party

@InitiatingFlow
class TestNotarisationFlow(val signers: List<Party> = emptyList()) : FlowLogic<SignedZKVerifierTransaction>() {

    @Suspendable
    override fun call(): SignedZKVerifierTransaction {

        val zkService: ZKTransactionService = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)

        val me = serviceHub.myInfo.legalIdentities.single().anonymise()
        val state = TestContract.TestState(me)
        val issueCommand = Command(TestContract.CreatePrivate(), (signers + me).map { it.owningKey }) //
        val stateAndContract = StateAndContract(state, TestContract.PROGRAM_ID)

        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(stateAndContract, issueCommand)

        val stx = subFlow(ZKCollectSignaturesFlow(serviceHub.signInitialTransaction(builder), signers.map { initiateFlow(it) }))

        val svtx = SignedZKVerifierTransaction(zkService.prove(stx.tx), stx.sigs)

        val notarySigs = subFlow(ZKNotaryFlow(stx, svtx))

        return svtx + notarySigs
    }
}
