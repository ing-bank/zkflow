package com.ing.zknotary.common.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.client.flows.ZKNotaryFlow
import com.ing.zknotary.client.flows.signInitialZKTransaction
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.transactions.SignedZKProverTransaction
import com.ing.zknotary.common.transactions.toSignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.toZKProverTransaction
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
class TestNotarisationFlow(val signers: List<Party> = emptyList()) : FlowLogic<Pair<SignedTransaction, SignedZKProverTransaction>>() {

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
        val stx = serviceHub.signInitialTransaction(builder)
        val ptx = stx.tx.toZKProverTransaction(
            serviceHub,
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_PROVER_TX_STORAGE),
            componentGroupLeafDigestService = BLAKE2s256DigestService,
            nodeDigestService = PedersenDigestService
        )

        val pztxSigs = signInitialZKTransaction(ptx)

        val sigs = subFlow(ZKCollectSignaturesFlow(stx, ptx.id, pztxSigs, signers.map { initiateFlow(it) }))

        // TODO very stupid way of doing this, better to use ptx that we already have, but will do for test only
        val vtx = stx.toSignedZKVerifierTransaction(
            serviceHub,
            sigs.zksigs,
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_PROVER_TX_STORAGE),
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE),
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)
        )

        val result = subFlow(ZKNotaryFlow(stx, vtx))

        return Pair(sigs.stx, SignedZKProverTransaction(ptx, sigs.zksigs + result))
    }
}
