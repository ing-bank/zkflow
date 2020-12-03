package com.ing.zknotary.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.notary.ZKNotarisationPayload
import com.ing.zknotary.notary.ZKNotarisationRequest
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowSession
import net.corda.core.flows.NotarisationRequestSignature
import net.corda.core.flows.NotarisationResponse
import net.corda.core.flows.NotaryError
import net.corda.core.flows.NotaryException
import net.corda.core.flows.NotaryFlow
import net.corda.core.identity.Party
import net.corda.core.internal.NetworkParametersStorage
import net.corda.core.internal.notary.validateSignatures
import net.corda.core.transactions.NotaryChangeWireTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap

open class ZKNotaryFlow(
    private val stx: SignedTransaction,
    private val zktx: SignedZKVerifierTransaction
) : NotaryFlow.Client(stx) {

    @Suspendable
    @Throws(NotaryException::class)
    override fun call(): List<TransactionSignature> {
        val notaryParty = checkTransaction()
        val response = zkNotarise(notaryParty)
        return validateNotaryResponse(response, notaryParty)
    }

    /** Notarises the transaction with the [notaryParty], obtains the notary's signature(s). */
    @Throws(NotaryException::class)
    @Suspendable
    protected fun zkNotarise(notaryParty: Party): UntrustworthyData<NotarisationResponse> {
        val session = initiateFlow(notaryParty)
        val requestSignature = generateRequestSignature()
        return if (isValidating(notaryParty)) {
            throw NotaryException(NotaryError.TransactionInvalid(Throwable("Validating notaries can never handle ZKTransactions")))
        } else {
            // TODO: find a way to check that this notary is actually running ZKNotaryServiceFlow (className property?)
            sendAndReceiveNonValidatingWithZKProof(notaryParty, session, requestSignature)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Suspendable
    private fun sendAndReceiveNonValidatingWithZKProof(
        notaryParty: Party,
        session: FlowSession,
        signature: NotarisationRequestSignature
    ): UntrustworthyData<NotarisationResponse> {
        session.send(ZKNotarisationPayload(zktx, signature))
        return receiveResultOrTiming(session)
    }

    /****************************************************
     * Copies of private methods from NotaryFlow.Client *
     ****************************************************/
    private fun isValidating(notaryParty: Party): Boolean {
        val onTheCurrentWhitelist = serviceHub.networkMapCache.isNotary(notaryParty)
        return if (!onTheCurrentWhitelist) {
            /*
                Note that the only scenario where it's acceptable to use a notary not in the current network parameter whitelist is
                when performing a notary change transaction after a network merge – the old notary won't be on the whitelist of the new network,
                and can't be used for regular transactions.
            */
            check(stx.coreTransaction is NotaryChangeWireTransaction) {
                "Notary $notaryParty is not on the network parameter whitelist. A non-whitelisted notary can only be used for notary change transactions"
            }
            val historicNotary =
                (serviceHub.networkParametersService as NetworkParametersStorage).getHistoricNotary(notaryParty)
                    ?: throw IllegalStateException("The notary party $notaryParty specified by transaction ${stx.id}, is not recognised as a current or historic notary.")
            historicNotary.validating
        } else serviceHub.networkMapCache.isValidatingNotary(notaryParty)
    }

    /**
     * Ensure that transaction ID instances are not referenced in the serialized form in case several input states are outputs of the
     * same transaction.
     */
    private fun generateRequestSignature(): NotarisationRequestSignature {
        // TODO: This is not required any more once our AMQP serialization supports turning off object referencing.
        val notarisationRequest =
            ZKNotarisationRequest(zktx.tx.inputs.map { it.copy(txhash = SecureHash.parse(it.txhash.toString())) }, zktx.id)
        return notarisationRequest.generateSignature(serviceHub)
    }

    /**
     * Checks that the notary's signature(s) is/are valid.
     *
     * This method is protected and final in original code (called validateResponse) that's why name change
     */
    private fun validateNotaryResponse(response: UntrustworthyData<NotarisationResponse>, notaryParty: Party): List<TransactionSignature> {
        return response.unwrap {
            it.validateSignatures(zktx.id, notaryParty)
            it.signatures
        }
    }
}
