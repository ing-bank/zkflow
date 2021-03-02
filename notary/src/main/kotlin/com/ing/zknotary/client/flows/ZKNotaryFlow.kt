package com.ing.zknotary.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.common.flows.FetchZKDataFlow
import com.ing.zknotary.common.flows.ZKDataVendingFlow
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.notary.ZKNotarisationPayload
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowSession
import net.corda.core.flows.NotarisationPayload
import net.corda.core.flows.NotarisationRequest
import net.corda.core.flows.NotarisationRequestSignature
import net.corda.core.flows.NotarisationResponse
import net.corda.core.flows.NotaryError
import net.corda.core.flows.NotaryException
import net.corda.core.flows.NotaryFlow
import net.corda.core.flows.SendTransactionFlow
import net.corda.core.identity.Party
import net.corda.core.internal.FlowIORequest
import net.corda.core.internal.checkPayloadIs
import net.corda.core.internal.notary.generateSignature
import net.corda.core.internal.notary.validateSignatures
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap

open class ZKNotaryFlow(
    stx: SignedTransaction,
    private val svtx: SignedZKVerifierTransaction
) : NotaryFlow.Client(stx) {

    @Suspendable
    @Throws(NotaryException::class)
    override fun call(): List<TransactionSignature> {
        val notaryParty = checkZKVerifierTransaction()
        val response = zkNotarise(notaryParty)
        return validateNotaryResponse(response, notaryParty)
    }

    /**
     * Checks that the transaction specifies a valid notary
     */
    @Suspendable
    protected fun checkZKVerifierTransaction(): Party {
        val notaryParty = svtx.tx.notary ?: error("Notary cannot be null")
        check(serviceHub.networkMapCache.isNotary(notaryParty)) { "$notaryParty is not a notary on the network" }
//      TODO check if states belong to this notary somehow (given that we don't have states' contents)
//        check(serviceHub.loadStates(svtx.tx.inputs.toSet() + svtx.tx.references.toSet()).all { it.state.notary == notaryParty }) {
//            "Input states and reference input states must have the same Notary"
//        }
        return notaryParty
    }

    /** Notarises the transaction with the [notaryParty], obtains the notary's signature(s). */
    @Throws(NotaryException::class)
    @Suspendable
    protected fun zkNotarise(notaryParty: Party): UntrustworthyData<NotarisationResponse> {
        val session = initiateFlow(notaryParty)
        val requestSignature = generateRequestSignature()
        return if (!isZKValidating(notaryParty)) {
            throw NotaryException(NotaryError.TransactionInvalid(Throwable("Notary is not ZK-validating")))
        } else {
            sendAndReceiveZKValidating(notaryParty, session, requestSignature)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Suspendable
    private fun sendAndReceiveZKValidating(
        notaryParty: Party,
        session: FlowSession,
        signature: NotarisationRequestSignature
    ): UntrustworthyData<NotarisationResponse> {
        val payload = ZKNotarisationPayload(svtx, signature)
        subFlow(ZKNotarySendTransactionFlow(session, payload))
        return receiveResultOrTiming(session)
    }

    /**
     * Currently we cannot check if notary is ZK-validating so we use dummy check
     */
    private fun isZKValidating(notaryParty: Party): Boolean {
        // TODO invent smart way of checking if notary is ZK validating
        return true
    }

    /**
     * Ensure that transaction ID instances are not referenced in the serialized form in case several input states are outputs of the
     * same transaction.
     */
    private fun generateRequestSignature(): NotarisationRequestSignature {
        // TODO: This is not required any more once our AMQP serialization supports turning off object referencing.
        val notarisationRequest =
            NotarisationRequest(svtx.tx.inputs.map { it.copy(txhash = it.txhash) }, svtx.id)
        return notarisationRequest.generateSignature(serviceHub)
    }

    /**
     * Checks that the notary's signature(s) is/are valid.
     */
    private fun validateNotaryResponse(response: UntrustworthyData<NotarisationResponse>, notaryParty: Party): List<TransactionSignature> {
        return response.unwrap {
            it.validateSignatures(svtx.id, notaryParty)
            it.signatures
        }
    }

    /**
     * The [NotarySendTransactionFlow] flow is similar to [SendTransactionFlow], but uses [NotarisationPayload] as the
     * initial message, and retries message delivery.
     */
    private class ZKNotarySendTransactionFlow(otherSide: FlowSession, payload: ZKNotarisationPayload) : ZKDataVendingFlow(otherSide, payload) {
        @Suspendable
        override fun sendPayloadAndReceiveDataRequest(otherSideSession: FlowSession, payload: Any): UntrustworthyData<FetchZKDataFlow.Request> {
            return otherSideSession.sendAndReceiveWithRetryCustom(payload)
        }

        @Suspendable
        private inline fun <reified R : Any> FlowSession.sendAndReceiveWithRetryCustom(payload: Any): UntrustworthyData<R> {
            return sendAndReceiveWithRetryCustom(R::class.java, payload)
        }

        @Suspendable
        private fun <R : Any> FlowSession.sendAndReceiveWithRetryCustom(receiveType: Class<R>, payload: Any): UntrustworthyData<R> {
            val request = FlowIORequest.SendAndReceive(
                sessionToMessage = stateMachine.serialize(mapOf(this to payload)),
                shouldRetrySend = true
            )
            return stateMachine.suspend(request, maySkipCheckpoint = false)[this]!!.checkPayloadIs(receiveType)
        }
    }
}
