package com.ing.zkflow.notary.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.flows.ResolveZKTransactionsFlow
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.dependencies
import com.ing.zkflow.common.transactions.verification.ZKTransactionVerifierService
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.notary.NotaryZKConfig
import com.ing.zkflow.notary.ZKNotarisationPayload
import com.ing.zkflow.notary.ZKNotaryService
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.NotarisationRequest
import net.corda.core.flows.NotarisationRequestSignature
import net.corda.core.flows.NotarisationResponse
import net.corda.core.flows.NotaryError
import net.corda.core.flows.NotaryException
import net.corda.core.flows.WaitTimeUpdate
import net.corda.core.identity.Party
import net.corda.core.internal.IdempotentFlow
import net.corda.core.internal.checkParameterHash
import net.corda.core.internal.notary.NotaryInternalException
import net.corda.core.internal.notary.UniquenessProvider
import net.corda.core.internal.notary.verifySignature
import net.corda.core.utilities.unwrap
import java.time.Duration

class ZKNotaryServiceFlow(
    val otherSideSession: FlowSession,
    val service: ZKNotaryService,
    private val etaThreshold: Duration,
    private val zkConfig: NotaryZKConfig
) : FlowLogic<Void?>(), IdempotentFlow {
    companion object {
        // TODO: Determine an appropriate limit and also enforce in the network parameters and the transaction builder.
        private const val MAX_ALLOWED_STATES_IN_TX = 10_000
    }

    init {
        if (service.services.networkParameters.minimumPlatformVersion < ZKFlow.REQUIRED_PLATFORM_VERSION) {
            throw IllegalStateException("The ZKNotaryService is compatible with Corda version ${ZKFlow.REQUIRED_PLATFORM_VERSION} or greater")
        }
    }

    @Suspendable
    override fun call(): Void? {
        val requestPayload = otherSideSession.receive<ZKNotarisationPayload>().unwrap { it }
        val tx = requestPayload.transaction.tx
        logger.info("Received a notarisation request for Tx [${tx.id}] from [${otherSideSession.counterparty.name}]")

        val commitStatus = try {
            validateTransactionSize(tx)
            validateNotary(tx)
            validateRequestSignature(tx, requestPayload.requestSignature)

            handleBackPressure(tx)

            verifyTransaction(requestPayload)

            service.commitStates(
                tx.inputs,
                tx.id,
                otherSideSession.counterparty,
                requestPayload.requestSignature,
                tx.timeWindow,
                tx.references
            )
        } catch (e: NotaryInternalException) {
            logger.error("Error notarising transaction ${tx.id}", e.error)
            // Any exception that's not a NotaryInternalException is assumed to be an unexpected internal com.ing.zkflow.generator.error
            // that is not relayed back to the client.
            throw NotaryException(e.error, tx.id)
        }

        if (commitStatus is UniquenessProvider.Result.Success) {
            // TODO: Why do we let the UniquenessProvider sign tx? Why not just let that return and sign the tx here?
            sendSignedResponse(tx.id, commitStatus.signature)
        } else {
            val error =
                java.lang.IllegalStateException("Request that failed uniqueness reached signing code! Ignoring.")
            throw NotaryException(NotaryError.General(error))
        }
        return null
    }

    private fun handleBackPressure(tx: ZKVerifierTransaction) {
        val eta = service.getEstimatedWaitTime(tx.inputs.size + tx.references.size + tx.outputHashes().size)
        // We don't have to check if counterparty can handle backpressure because we already require
        // platform version >= MIN_PLATFORM_VERSION_FOR_BACKPRESSURE_MESSAGE anyway
        if (eta > etaThreshold) {
            otherSideSession.send(WaitTimeUpdate(eta))
        }
    }

    /** Verifies that the correct notarisation request was signed by the counterparty. */
    private fun validateRequestSignature(tx: ZKVerifierTransaction, signature: NotarisationRequestSignature) {
        val request = NotarisationRequest(tx.inputs.map { it.copy(txhash = it.txhash) }, tx.id)
        val requestingParty = otherSideSession.counterparty
        request.verifySignature(signature, requestingParty)
    }

    private fun validateTransactionSize(tx: ZKVerifierTransaction) {
        try {
            // TODO: should this include outputs?
            checkMaxStateCount(tx.inputs + tx.references)
        } catch (e: IllegalArgumentException) {
            throw NotaryInternalException(NotaryError.TransactionInvalid(e))
        }
    }

    private fun validateNotary(tx: ZKVerifierTransaction) {
        try {
            val notary = tx.notary ?: throw IllegalArgumentException("Transaction does not specify a notary.")
            checkTxNotaryIsMe(notary)
            /**
             * Not calling [checkParameterHash] anymore: that same operation is already done by [checkNotaryWhitelisted]
             */
            checkNotaryWhitelisted(notary, tx.networkParametersHash)
        } catch (e: IllegalArgumentException) {
            throw NotaryInternalException(NotaryError.TransactionInvalid(e))
        }
    }

    @Suspendable
    private fun sendSignedResponse(txId: SecureHash, signature: TransactionSignature) {
        logger.info("Transaction [$txId] successfully notarised, sending signature back to [${otherSideSession.counterparty.name}]")
        otherSideSession.send(NotarisationResponse(listOf(signature)))
    }

    /** Check if transaction is intended to be signed by this notary. */
    @Suspendable
    private fun checkTxNotaryIsMe(notary: Party) {
        require(notary.owningKey == service.notaryIdentityKey) {
            "The notary specified on the transaction: [$notary] does not match the notary service's identity: [${service.notaryIdentityKey}] "
        }
    }

    /** Checks whether the number of input states is too large. */
    private fun checkMaxStateCount(states: List<StateRef>) {
        require(states.size < MAX_ALLOWED_STATES_IN_TX) {
            "A transaction cannot have more than $MAX_ALLOWED_STATES_IN_TX " +
                "inputs or references, received: ${states.size}"
        }
    }

    @Suspendable
    private fun verifyTransaction(requestPayload: ZKNotarisationPayload) {
        val svtx = requestPayload.transaction

        try {
            /**
             * TODO Check if we need to do some more checks from [TransactionVerifierServiceInternal]
             * that are done on Ledgertransactions
             */
            // Resolve dependencies
            subFlow(ResolveZKTransactionsFlow(svtx.tx, svtx.dependencies, otherSideSession))

            // Verify ZKP
            val zkTransactionVerifierService = ZKTransactionVerifierService(serviceHub, zkConfig.zkTransactionService)
            zkTransactionVerifierService.verify(svtx, false)
            // Verify signatures, we cannot do it inside zkTransactionVerifierService.verify because it lacks our (notary) signature
            svtx.verifySignaturesExcept(service.notaryIdentityKey)
        } catch (e: IllegalArgumentException) {
            throw NotaryInternalException(NotaryError.TransactionInvalid(e))
        }
    }

    /** Make sure the transaction notary is part of the network parameter whitelist. */
    private fun checkNotaryWhitelisted(notary: Party, attachedParameterHash: SecureHash?) {
        // Expecting network parameters to be attached for platform version 4 or later.
        if (attachedParameterHash == null) {
            throw IllegalArgumentException("Transaction must contain network parameters.")
        }
        val attachedParameters = serviceHub.networkParametersService.lookup(attachedParameterHash)
            ?: throw IllegalStateException("Unable to resolve network parameters from hash: $attachedParameterHash")

        val notaryWhitelist = attachedParameters.notaries.map { it.identity }

        check(notary in notaryWhitelist) {
            "Notary specified by the transaction ($notary) is not on the network parameter whitelist: ${notaryWhitelist.joinToString()}"
        }
    }
}
