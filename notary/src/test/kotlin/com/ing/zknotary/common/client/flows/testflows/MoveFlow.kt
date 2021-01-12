package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.createSignature
import com.ing.zknotary.client.flows.signInitialZKTransaction
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

/**
 * Disclaimer: this is not how it is supposed to be used in "real" flows, it works just for this test
 * TODO Verifier should rebuild ZKTX basing on moveStx but for now its complicated so it is temporary skipped
 */
@InitiatingFlow
class MoveFlow(
    private val createStx: SignedTransaction,
    private val newOwner: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val zkService: ZKTransactionService = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)

        val session = initiateFlow(newOwner)

        val me = serviceHub.myInfo.legalIdentities.single()
        val state = createStx.coreTransaction.outRef<TestContract.TestState>(0)
        val command = Command(TestContract.Move(), listOf(newOwner, me).map { it.owningKey })
        val stateAndContract = StateAndContract(state.state.data.copy(owner = newOwner), TestContract.PROGRAM_ID)

        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single())
        builder.withItems(state, stateAndContract, command)
        builder.toWireTransaction(serviceHub).toLedgerTransaction(serviceHub).verify()

        // Transaction creator signs transaction.
        val stx = serviceHub.signInitialTransaction(builder)

        val zktx = zkService.prove(stx.tx)

        val pztxSigs = signInitialZKTransaction(zktx)
        val zkpstx = SignedZKVerifierTransaction(zktx, pztxSigs)

        // Help Verifier to resolve dependencies for normal tx
        subFlow(SendZKTransactionFlow(session, zkpstx))

        val fullySignedZktx = session.receive<SignedZKVerifierTransaction>().unwrap { it }

        zkService.verify(fullySignedZktx)

        // We don't call FinalityFlow here because it expects "normal" Stx to be signed as well
        //  and in current implementation of backchain we don't operate "normal" txs.
        //  This may change later.

        return stx
    }

    companion object {

        @InitiatedBy(MoveFlow::class)
        class Verifier(val session: FlowSession) : FlowLogic<Unit>() {

            @Suspendable
            override fun call() {
                val zkService: ZKTransactionService = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)

                // Receive TX and verify ZK backchain
                val zktx = subFlow(ReceiveZKTransactionFlow(session))

                val key = serviceHub.keyManagementService.filterMyKeys(zktx.requiredSigningKeys).single()
                val fullySignedZktx = SignedZKVerifierTransaction(zktx.tx, zktx.sigs + serviceHub.createSignature(zktx.id, key))

                zkService.verify(fullySignedZktx)

                session.send(fullySignedZktx)
            }
        }
    }
}
