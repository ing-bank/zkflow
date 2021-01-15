package com.ing.zknotary.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.notary.ZKNotarisationPayload
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowSession
import net.corda.core.flows.NotarisationRequest
import net.corda.core.flows.NotarisationRequestSignature
import net.corda.core.flows.NotarisationResponse
import net.corda.core.flows.NotaryError
import net.corda.core.flows.NotaryException
import net.corda.core.flows.NotaryFlow
import net.corda.core.identity.Party
import net.corda.core.internal.notary.generateSignature
import net.corda.core.internal.notary.validateSignatures
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap

open class ZKNotaryFlow(
    private val stx: SignedTransaction,
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
        val notaryParty = svtx.tx.notary
        check(serviceHub.networkMapCache.isNotary(notaryParty)) { "$notaryParty is not a notary on the network" }
        check(serviceHub.loadStates(svtx.tx.inputs.toSet() + svtx.tx.references.toSet()).all { it.state.notary == notaryParty }) {
            "Input states and reference input states must have the same Notary"
        }
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
        session.send(ZKNotarisationPayload(svtx, signature))
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
            NotarisationRequest(svtx.tx.inputs.map { it.copy(txhash = SecureHash.parse(it.txhash.toString())) }, svtx.id)
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
}
