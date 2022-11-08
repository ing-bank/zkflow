/*
 * Source attribution:
 *
 * Some flows in this file are strongly based on their original non-ZKP counterpart (i.e. without the 'ZK' prefix in the class name) from Corda
 * itself, as defined in the package net.corda.core.flows (https://github.com/corda/corda).
 *
 * Ideally ZKFlow could have extended the Corda flows to add the ZKP checks only, and leave the rest of the behaviour intact.
 * Unfortunately, Corda's flows were not implemented with extension in mind, and it was not possible to create this flow without copying most
 * of the original flow.
 */
package com.ing.zkflow.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.network.ZKNetworkParametersServiceLoader
import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.resolvePublicOrPrivateStateRef
import com.ing.zkflow.notary.ZKNotarisationPayload
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
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
import net.corda.core.internal.SerializedStateAndRef
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
        val notaryParty = svtx.tx.notary ?: throw IllegalStateException("Transaction does not specify a Notary")
        check(serviceHub.networkMapCache.isNotary(notaryParty)) { "$notaryParty is not a notary on the network" }
        ZKNetworkParametersServiceLoader.latest.notaryInfo.validate(notaryParty)
        check(fetchStates(svtx.tx.inputs.toSet() + svtx.tx.references.toSet()).all { it.state.notary == notaryParty }) {
            "Input states and reference input states must have the same Notary"
        }
        return notaryParty
    }

    private fun fetchStates(states: Set<StateRef>): Set<StateAndRef<*>> {
        return states.map {
            SerializedStateAndRef(
                resolvePublicOrPrivateStateRef(
                    it,
                    serviceHub,
                    serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE),
                    serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_UTXO_INFO_STORAGE)
                ),
                it
            ).toStateAndRef()
        }.toSet()
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
     * This will return true if the notary is a normal notary. Without changes to Corda it is not possible to determine more information
     * about a notary.
     *
     * If the notary is not actually a ZKNotary, this will still cause a notarisation error a bit later in the notarisation process.
     * This is acceptable because it is 1) still runtime, and 2) does not introduce any security issue.s
     *
     */
    private fun isZKValidating(notaryParty: Party): Boolean {
        return serviceHub.networkMapCache.isNotary(notaryParty)
    }

    /**
     * Ensure that transaction ID instances are not referenced in the serialized form in case several input states are outputs of the
     * same transaction.
     */
    private fun generateRequestSignature(): NotarisationRequestSignature {
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
    private class ZKNotarySendTransactionFlow(otherSide: FlowSession, payload: ZKNotarisationPayload) :
        ZKDataVendingFlow(otherSide, payload) {
        @Suspendable
        override fun sendPayloadAndReceiveDataRequest(
            otherSideSession: FlowSession,
            payload: Any
        ): UntrustworthyData<FetchZKDataFlow.Request> {
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
