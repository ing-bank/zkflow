package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKCollectSignaturesFlow
import com.ing.zknotary.client.flows.ZKFinalityFlow
import com.ing.zknotary.client.flows.ZKReceiveFinalityFlow
import com.ing.zknotary.client.flows.ZKSignTransactionFlow
import com.ing.zknotary.client.flows.signInitialZKTransaction
import com.ing.zknotary.common.flows.ResolveZKTransactionsFlow
import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import com.ing.zknotary.common.transactions.collectSerializedUtxosAndNonces
import com.ing.zknotary.common.transactions.signInitialTransaction
import com.ing.zknotary.common.zkp.UtxoInfo
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import com.ing.zknotary.testing.fixtures.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

/**
 * Disclaimer: this is not how it is supposed to be used in "real" flows, it works just for this test
 */
@InitiatingFlow
class MoveBidirectionalFlow(
    private val myInput: StateAndRef<TestContract.TestState>,
//    private val theirInput: StateAndRef<TestContract.TestState>,
//    private val receivedSerializedInput: ByteArray,
//    private val receivedInputNonce: SecureHash,
    private val counterParty: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val zkService: ZKTransactionService = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)
        val zkTxStorage: ZKVerifierTransactionStorage = serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)
        val session = initiateFlow(counterParty)

        // Initiator sends proposed state to exchange.
        // Expects UtxoInfo for a state of the same value in return.
        val theirUtxoInfo = session.sendAndReceive<UtxoInfo>(myInput).unwrap { it }

        // resolve backchain for theirUtxoInfo
        subFlow(ResolveZKTransactionsFlow(null, setOf(theirUtxoInfo.stateRef.txhash), session))

        // calculated_utxo_hash =  hash(receivedSerializedInput +receivedInputNonce)
        val calculatedUtxoHash = SecureHash.componentHashAs(theirUtxoInfo.digestAlgorithm, theirUtxoInfo.nonce.copyBytes() + theirUtxoInfo.serializedContents)

        val resolvedOutput = zkTxStorage.getTransaction(theirUtxoInfo.stateRef.txhash)?.tx?.outputHashes?.get(0) ?: error("Coulnd't resolve zkvtx with id ${theirUtxoInfo.stateRef.txhash}")

        // resolved_vtx.outref(theirInput.ref) == calculated_utxo_hash
        require(resolvedOutput == calculatedUtxoHash) { "Calculated UTXO hash '$calculatedUtxoHash' does not match resolved hash '$resolvedOutput'" }

        val theirInput = theirUtxoInfo.serializedContents.deserialize<TransactionState<TestContract.TestState>>()

        // Now we create the transaction
        val me = serviceHub.myInfo.legalIdentities.single()
        val command = Command(TestContract.Move(), listOf(counterParty, me).map { it.owningKey })
        val myOutput = StateAndContract(myInput.state.data.copy(owner = counterParty), TestContract.PROGRAM_ID)
        val theirOutput = StateAndContract(theirInput.data.copy(owner = me), TestContract.PROGRAM_ID)

        val builder = ZKTransactionBuilder(TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single()))
        builder.withItems(myInput, theirInput, myOutput, theirOutput, command)

        // Transaction creator signs transaction.
        val stx = serviceHub.signInitialTransaction(builder)

        // TODO: this fails for now, because we don't have the tx history for theirInput.
        // Will this work when we have executed the DataVendingFlow first? Or still not?
        // more logically, we will do vtx.verify(), where we get the hashes for the inputs/referecens from
        // the resolved zkvtransactions
//        stx.verify(serviceHub, false)

        val vtx = zkService.prove(stx.tx, listOf(theirUtxoInfo))

        val partiallySignedVtx = signInitialZKTransaction(vtx)
        val svtx = subFlow(ZKCollectSignaturesFlow(stx, partiallySignedVtx, listOf(session)))

        subFlow(ZKFinalityFlow(stx, svtx, listOf(session)))

        return stx
    }

    companion object {

        @InitiatedBy(MoveBidirectionalFlow::class)
        class Verifier(val session: FlowSession) : FlowLogic<Unit>() {
            @Suspendable
            override fun call() {
                val signFlow = object : ZKSignTransactionFlow(session) {
                    @Suspendable
                    override fun checkTransaction(stx: SignedTransaction) = requireThat {
                        // In non-test scenario here counterparty can verify incoming Tx from business perspective
                    }
                }
                val initiatorState = session.receive<StateAndRef<TestContract.TestState>>().unwrap { it }

                // Create a state of the same value as the one proposed by initiator
                val createStx = subFlow(CreateFlow(initiatorState.state.data.value))
                // Send it back.

                val output = createStx.tx.outRef<TestContract.TestState>(0)
                val (serialized, nonces) = serviceHub.collectSerializedUtxosAndNonces(listOf(output.ref))
                val utxoInfo = UtxoInfo(
                    stateRef = output.ref,
                    serializedContents = serialized.single(),
                    nonce = nonces.single(),
                    digestAlgorithm = createStx.tx.digestService.hashAlgorithm
                )
                session.send(utxoInfo)

                // Invoke the signing subFlow, in response to the counterparty calling [ZKCollectSignaturesFlow].
                val stx = subFlow(signFlow)

                // Invoke flow in response to ZKFinalityFlow
                subFlow(ZKReceiveFinalityFlow(session, stx))
            }
        }
    }
}
