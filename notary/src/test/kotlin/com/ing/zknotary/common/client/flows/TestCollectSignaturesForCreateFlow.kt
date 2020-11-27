package com.ing.zknotary.common.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.client.flows.ZKSignTransactionFlow
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.transactions.SignedZKProverTransaction
import com.ing.zknotary.common.transactions.toZKProverTransaction
import com.ing.zknotary.node.services.MockZKProverTransactionStorage
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
class TestCollectSignaturesForCreateFlow(val signers: List<Party> = emptyList()) : FlowLogic<Pair<SignedTransaction, SignedZKProverTransaction>>() {

    @Suspendable
    override fun call(): Pair<SignedTransaction, SignedZKProverTransaction> {

        val me = serviceHub.myInfo.legalIdentities.single()
        val state = TestContract.TestState(me)
        val issueCommand = Command(TestContract.Create(), (signers + me).map { it.owningKey }) //
        val stateAndContract = StateAndContract(state, TestContract.PROGRAM_ID)

        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(stateAndContract, issueCommand)
        builder.toWireTransaction(serviceHub).toLedgerTransaction(serviceHub).verify()

        // Transaction creator signs transaction.
        val ptx = serviceHub.signInitialTransaction(builder)
        val ztx = ptx.tx.toZKProverTransaction(
            serviceHub,
            serviceHub.cordaService(MockZKProverTransactionStorage::class.java),
            componentGroupLeafDigestService = BLAKE2s256DigestService,
            nodeDigestService = PedersenDigestService
        )

        val pztxSigs = signInitialZKTransaction(ztx)

        val sigs = subFlow(ZKCollectSignaturesFlow(ptx, ztx.id, pztxSigs, signers.map { initiateFlow(it) }))

        return Pair(sigs.stx, SignedZKProverTransaction(ztx, sigs.zksigs))
    }
}

@InitiatedBy(TestCollectSignaturesForCreateFlow::class)
class CounterpartySigner(val otherPartySession: FlowSession) : FlowLogic<Any>() {

    @Suspendable
    override fun call(): Any {
        val flow = object : ZKSignTransactionFlow(otherPartySession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // In non-test scenario here counterparty should verify incoming Tx including ZK Merkle tree calculation
            }
        }

        // Invoke the subFlow, in response to the counterparty calling [ZKCollectSignaturesFlow].
        subFlow(flow)

        return 0
    }
}
