package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.TransactionsPair
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.client.flows.ZKSignTransactionFlow
import com.ing.zknotary.client.flows.signInitialZKTransaction
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.crypto.blake2s256
import com.ing.zknotary.common.crypto.pedersen
import com.ing.zknotary.common.transactions.SignedZKProverTransaction
import com.ing.zknotary.common.transactions.toZKProverTransaction
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.DigestService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
class TestCollectZKSignaturesFlow(val signers: List<Party> = emptyList()) : FlowLogic<TransactionsPair>() {

    @Suspendable
    override fun call(): TransactionsPair {

        val zkService = serviceHub.getCordaServiceFromConfig<ZKTransactionService>(ServiceNames.ZK_TX_SERVICE)
        val me = serviceHub.myInfo.legalIdentities.single()
        val state = TestContract.TestState(me)
        val issueCommand = Command(TestContract.Create(), (signers + me).map { it.owningKey }) //
        val stateAndContract = StateAndContract(state, TestContract.PROGRAM_ID)

        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(stateAndContract, issueCommand)
        builder.toWireTransaction(serviceHub).toLedgerTransaction(serviceHub).verify()

        // Sign plaintext transaction
        val stx = serviceHub.signInitialTransaction(builder)

        // Prepare ZKP transaction
        val ptx = stx.tx.toZKProverTransaction(
            serviceHub,
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE),
            componentGroupLeafDigestService = DigestService.blake2s256,
            nodeDigestService = DigestService.pedersen
        )
        val vtx = zkService.prove(ptx)

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
