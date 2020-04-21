package com.ing.zknotary.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKFinalityFlow
import com.ing.zknotary.client.flows.ZKNotaryFlow
import com.ing.zknotary.common.zkp.ZKConfig
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.CollectSignatureFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.NotaryException
import net.corda.core.flows.NotaryFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.javaConstructor

// This custom ZK notary client flow does not check the validity the transaction here as normal in NotaryFlow.Client,
// because that would fail: the tx is invalid on purpose, so that we can confirm that the notary rejects or doesn't reject an invalid tx.
// Other than that, it is an unmodified copy of NotaryFlow.Client.
class ZKNonTxCheckingNotaryClientFlow(private val stx: SignedTransaction, zkConfig: ZKConfig) : ZKNotaryFlow(stx, zkConfig) {
    @Suspendable
    @Throws(NotaryException::class)
    override fun call(): List<TransactionSignature> {
        // We don't check the transaction here as normal in ZKNotaryFlow, because that would fail:
        // the tx is invalid on purpose, so that we can confirm that the notary rejects or doesn't reject an invalid tx.
        val notaryParty = stx.notary ?: throw IllegalStateException("Transaction does not specify a Notary")
        val response = zkNotarise(notaryParty)
        return validateResponse(response, notaryParty)
    }
}

// This custom notary client flow does not check the validity the transaction here as normal in NotaryFlow.Client,
// because that would fail: the tx is invalid on purpose, so that we can confirm that the notary rejects or doesn't reject an invalid tx.
// Other than that, it is an unmodified copy of NotaryFlow.Client.
class NonTxCheckingNotaryClientFlow(private val stx: SignedTransaction) : NotaryFlow.Client(stx) {
    @Suspendable
    @Throws(NotaryException::class)
    override fun call(): List<TransactionSignature> {
        // We don't check the transaction here as normal in NotaryFlow.Client, because that would fail:
        // the tx is invalid on purpose, so that we can confirm that the notary rejects or doesn't reject an invalid tx.
        val notaryParty = stx.notary ?: throw IllegalStateException("Transaction does not specify a Notary")
        val response = notarise(notaryParty)
        return validateResponse(response, notaryParty)
    }
}

@InitiatingFlow
class ZKMoveFlow(
    private val stx: SignedTransaction,
    private val newOwner: Party,
    private val zkConfig: ZKConfig
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val newOwnerSession = initiateFlow(newOwner)
        val allSignedTx =
            stx + subFlow(CollectSignatureFlow(stx, newOwnerSession, newOwnerSession.counterparty.owningKey))
        val flow = ZKFinalityFlow(
            allSignedTx,
            listOf(newOwnerSession),
            zkConfig = zkConfig
        )
        return subFlow(flow)
    }
}

@InitiatingFlow
class MoveFlow<T : FlowLogic<SignedTransaction>>(
    private val stx: SignedTransaction,
    private val newOwner: Party,
    finalityFlow: KClass<T>
) : FlowLogic<SignedTransaction>() {

    private val finalityFlowConstructor = finalityFlow.constructors.single {
        it.visibility == KVisibility.PUBLIC &&
            it.parameters.size == 3 &&
            it.parameters[0].type.classifier == SignedTransaction::class &&
            it.parameters[1].type.classifier == FlowSession::class &&
            it.parameters[2].type.classifier == Array<out FlowSession>::class
    }.javaConstructor!!

    @Suspendable
    override fun call(): SignedTransaction {
        val newOwnerSession = initiateFlow(newOwner)
        val allSignedTx =
            stx + subFlow(CollectSignatureFlow(stx, newOwnerSession, newOwnerSession.counterparty.owningKey))
        val flow = finalityFlowConstructor.newInstance(
            allSignedTx,
            newOwnerSession,
            emptyList<FlowSession>().toTypedArray()
        )
        return subFlow(flow)
    }
}

@InitiatedBy(ZKMoveFlow::class)
class ZKMoveReplyFlow(val otherSideSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(otherSideSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Verify that we know who all the participants in the transaction are
                val states: Iterable<ContractState> =
                    serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data } + stx.tx.outputs.map { it.data }
                states.forEach { state ->
                    state.participants.forEach { anon ->
                        require(serviceHub.identityService.wellKnownPartyFromAnonymous(anon) != null) {
                            "Transaction state $state involves unknown participant $anon"
                        }
                    }
                }
            }
        }

        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(otherSideSession, expectedTxId = txId))
    }
}

@InitiatedBy(MoveFlow::class)
class MoveReplyFlow(val otherSideSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(otherSideSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Verify that we know who all the participants in the transaction are
                val states: Iterable<ContractState> =
                    serviceHub.loadStates(stx.tx.inputs.toSet()).map { it.state.data } + stx.tx.outputs.map { it.data }
                states.forEach { state ->
                    state.participants.forEach { anon ->
                        require(serviceHub.identityService.wellKnownPartyFromAnonymous(anon) != null) {
                            "Transaction state $state involves unknown participant $anon"
                        }
                    }
                }
            }
        }

        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(otherSideSession, expectedTxId = txId))
    }
}
