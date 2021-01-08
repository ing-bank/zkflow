package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.client.flows.ZKNotaryFlow
import com.ing.zknotary.client.flows.signInitialZKTransaction
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.transactions.SignedZKProverTransaction
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.toZKProverTransaction
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKWritableProverTransactionStorage
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
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
class NotarisationFlow(val signers: List<Party> = emptyList()) : FlowLogic<Pair<SignedTransaction, SignedZKProverTransaction>>() {

    @Suspendable
    override fun call(): Pair<SignedTransaction, SignedZKProverTransaction> {

        val zkService: ZKTransactionService = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)
        val zkProverTransactionStorage: ZKWritableProverTransactionStorage = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_PROVER_TX_STORAGE)
        val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)

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
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE),
            componentGroupLeafDigestService = BLAKE2s256DigestService,
            nodeDigestService = PedersenDigestService
        )

        val initialPtxSigs = signInitialZKTransaction(ptx)

        val sigs = subFlow(ZKCollectSignaturesFlow(stx, ptx.id, initialPtxSigs, signers.map { initiateFlow(it) }))

        // TODO Inside prove() currently we create PTX one more time what affects performance. To remove that we should either
        //  1) be able to send PTX to ZKService OR
        //  2) remove ptxId from ZXCollectSignatures flow which makes sense because counterparty recalculates it anyway
        val vtx = zkService.prove(stx.tx)

        val sptx = SignedZKProverTransaction(ptx, sigs.zksigs)
        val svtx = SignedZKVerifierTransaction(vtx, sigs.zksigs)

        // Persist zk transactions
        // TODO Do we really need to store PTX at all? It seems to be just temporary form of only required for proving
        zkProverTransactionStorage.map.put(stx, ptx)
        zkVerifierTransactionStorage.map.put(stx, vtx)
        zkProverTransactionStorage.addTransaction(sptx)
        zkVerifierTransactionStorage.addTransaction(svtx)

        val result = subFlow(ZKNotaryFlow(stx, svtx))

        return Pair(sigs.stx, SignedZKProverTransaction(ptx, sigs.zksigs + result))
    }
}
