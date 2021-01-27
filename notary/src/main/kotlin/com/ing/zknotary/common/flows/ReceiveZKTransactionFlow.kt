package com.ing.zknotary.common.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.recordTransactions
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.dependencies
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.SendStateAndRefFlow
import net.corda.core.internal.checkParameterHash
import net.corda.core.node.StatesToRecord
import net.corda.core.utilities.trace
import net.corda.core.utilities.unwrap
import java.security.SignatureException
import net.corda.core.contracts.TransactionResolutionException as TransactionResolutionException1

/**
 * The [ReceiveTransactionFlow] should be called in response to the [SendTransactionFlow].
 *
 * This flow is a combination of [FlowSession.receive], resolve and [SignedZKVerifierTransaction.verify]. This flow will receive the
 * [SignedZKVerifierTransaction] and perform the resolution back-and-forth required to check the dependencies and download any missing
 * attachments. The flow will return the [SignedZKVerifierTransaction] after it is resolved and then verified using [SignedZKVerifierTransaction.verify].
 *
 * Please note that it will *not* store the transaction to the vault unless that is explicitly requested and checkSufficientSignatures is true.
 * Setting statesToRecord to anything else when checkSufficientSignatures is false will *not* update the vault.
 *
 * Attention: At the moment, this flow receives a [SignedZKVerifierTransaction] first thing and then proceeds by invoking a [ResolveTransactionsFlow] subflow.
 *            This is used as a criterion to identify cases, where a counterparty has failed notarising a transact
 *
 * @property otherSideSession session to the other side which is calling [SendTransactionFlow].
 * @property checkSufficientSignatures if true checks all required signatures are present. See [SignedZKVerifierTransaction.verify].
 * @property statesToRecord which transaction states should be recorded in the vault, if any.
 */
open class ReceiveZKTransactionFlow @JvmOverloads constructor(
    private val otherSideSession: FlowSession,
    private val checkSufficientSignatures: Boolean = true,
    private val statesToRecord: StatesToRecord = StatesToRecord.NONE
) : FlowLogic<SignedZKVerifierTransaction>() {

    @Suppress("LongMethod", "ComplexMethod")
    @Suspendable
    @Throws(
        SignatureException::class,
        AttachmentResolutionException::class,
        TransactionResolutionException1::class,
        TransactionVerificationException::class
    )
    override fun call(): SignedZKVerifierTransaction {
        if (checkSufficientSignatures) {
            logger.trace { "Receiving a transaction from ${otherSideSession.counterparty}" }
        } else {
            logger.trace { "Receiving a transaction (but without checking the signatures) from ${otherSideSession.counterparty}" }
        }
        val zkService = serviceHub.getCordaServiceFromConfig<ZKTransactionService>(ServiceNames.ZK_TX_SERVICE)
        val svtx = otherSideSession.receive<SignedZKVerifierTransaction>().unwrap {
//            it.pushToLoggingContext()
            logger.info("Received transaction acknowledgement request from party ${otherSideSession.counterparty}.")
            checkParameterHash(it.tx.networkParametersHash)
            subFlow(ResolveZKTransactionsFlow(it.dependencies, otherSideSession))
            logger.info("Transaction dependencies resolution completed.")
            try {
                zkService.verify(it, checkSufficientSignatures)
                it
            } catch (e: Exception) {
                logger.warn("Transaction verification failed.")
                throw e
            }
        }
        if (checkSufficientSignatures) {
            // We should only send a transaction to the vault for processing if we did in fact fully verify it, and
            // there are no missing signatures. We don't want partly signed stuff in the vault.
            checkBeforeRecording(svtx)
            logger.info("Successfully received fully signed tx. Sending it to the vault for processing.")
            serviceHub.recordTransactions(null, svtx)
            logger.info("Successfully recorded received transaction locally.")
        }
        return svtx
    }

    /**
     * Hook to perform extra checks on the received transaction just before it's recorded. The transaction has already
     * been resolved and verified at this point.
     */
    @Suspendable
    @Throws(FlowException::class)
    protected open fun checkBeforeRecording(stx: SignedZKVerifierTransaction) = Unit
}

/**
 * The [ReceiveStateAndRefFlow] should be called in response to the [SendStateAndRefFlow].
 *
 * This flow is a combination of [FlowSession.receive] and resolve. This flow will receive a list of [StateAndRef]
 * and perform the resolution back-and-forth required to check the dependencies.
 * The flow will return the list of [StateAndRef] after it is resolved.
 */
// @JvmSuppressWildcards is used to suppress wildcards in return type when calling `subFlow(new ReceiveStateAndRef<T>(otherParty))` in java.
class ReceiveStateAndRefFlow<out T : ContractState>(private val otherSideSession: FlowSession) :
    FlowLogic<@JvmSuppressWildcards List<StateAndRef<T>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<T>> {
        return otherSideSession.receive<List<StateAndRef<T>>>().unwrap {
            val txHashes = it.asSequence().map { it.ref.txhash }.toSet()
            subFlow(ResolveZKTransactionsFlow(txHashes, otherSideSession))
            it
        }
    }
}
