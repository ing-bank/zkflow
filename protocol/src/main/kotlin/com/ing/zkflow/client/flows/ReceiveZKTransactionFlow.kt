package com.ing.zkflow.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.flows.ResolveZKTransactionsFlow
import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.WritableUtxoInfoStorage
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.transactions.NotarisedTransactionPayload
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.UtxoInfo
import com.ing.zkflow.common.transactions.dependencies
import com.ing.zkflow.common.transactions.fetchMissingAttachments
import com.ing.zkflow.common.transactions.verification.zkVerify
import com.ing.zkflow.common.zkp.ZKTransactionService
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.internal.checkParameterHash
import net.corda.core.node.StatesToRecord
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.trace
import net.corda.core.utilities.unwrap
import java.security.SignatureException

/**
 * The [ZKReceiveNotarisedTransactionPayloadFlow] should be called in response to the [SendNotarisedTransactionPayloadFlow].
 *
 * This flow is a combination of [FlowSession.receive], resolve and [SignedZKVerifierTransaction.verify]. This flow will receive the
 * [SignedZKVerifierTransaction] and perform the resolution back-and-forth required to check the dependencies and download any missing
 * attachments. The flow will return the [SignedZKVerifierTransaction] after it is resolved and then verified using [SignedZKVerifierTransaction.verify].
 *
 * Please note that it will *not* store the transaction to the vault unless that is explicitly requested and checkSufficientSignatures is true.
 * Setting statesToRecord to anything else when checkSufficientSignatures is false will *not* update the vault.
 *
 * @property otherSideSession session to the other side which is calling [SendTransactionFlow].
 * @property checkSufficientSignatures if true checks all required signatures are present. See [SignedZKVerifierTransaction.verify].
 */
open class ZKReceiveNotarisedTransactionPayloadFlow @JvmOverloads constructor(
    private val otherSideSession: FlowSession,
    private val checkSufficientSignatures: Boolean = true,
    private val statesToRecord: StatesToRecord = StatesToRecord.ONLY_RELEVANT
) : FlowLogic<SignedZKVerifierTransaction>() {

    @Suppress("LongMethod", "ComplexMethod")
    @Suspendable
    @Throws(
        SignatureException::class,
        AttachmentResolutionException::class,
        TransactionResolutionException::class,
        TransactionVerificationException::class
    )
    override fun call(): SignedZKVerifierTransaction {
        if (checkSufficientSignatures) {
            logger.trace { "Receiving a transaction from ${otherSideSession.counterparty}" }
        } else {
            logger.trace { "Receiving a transaction (but without checking the signatures) from ${otherSideSession.counterparty}" }
        }
        val notarised = otherSideSession.receive<NotarisedTransactionPayload>()
            .unwrap { notarised ->
                logger.info("Received transaction acknowledgement request from party ${otherSideSession.counterparty}.")
                checkParameterHash(notarised.svtx.tx.networkParametersHash)

                subFlow(ResolveZKTransactionsFlow(notarised.svtx.tx, notarised.svtx.dependencies, otherSideSession))
                logger.info("Transaction dependencies resolution completed.")

                try {
                    notarised.verify(
                        serviceHub,
                        serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE),
                        checkSufficientSignatures
                    )
                    notarised
                } catch (e: Exception) {
                    logger.warn("Transaction verification failed.")
                    throw e
                }
            }
        if (checkSufficientSignatures) {
            // We should only send a transaction to the vault for processing if we did in fact fully verify it, and
            // there are no missing signatures. We don't want partly signed stuff in the vault.
            checkBeforeRecording(notarised)
            logger.info("Successfully received fully signed tx. Sending it to the vault for processing.")
            serviceHub.recordTransactions(notarised, statesToRecord)
            logger.info("Successfully recorded received transaction locally.")
        }
        return notarised.svtx
    }

    /**
     * Hook to perform extra checks on the received transaction just before it's recorded. The transaction has already
     * been resolved and verified at this point.
     */
    @Suspendable
    protected open fun checkBeforeRecording(notarised: NotarisedTransactionPayload) {
    }
}

/**
 * This is the counterpart to [ZKSendTransactionProposal].
 *
 * It should *only* be used for receiving transaction proposals, not for receiving finalized transactions.
 *
 * Use [ZKReceiveNotarisedTransactionPayloadFlow] and its counterpart [SendNotarisedTransactionPayloadFlow] for sending finalized
 * transactions together with their ZKP variants for storage.
 *
 * This receives an [SignedTransaction] for further processing verifies it and resolves and verifies its backchain.
 * It does *not* store the [SignedTransaction] itself, only its verified ZKP backchain(s)
 */
open class ZKReceiveTransactionProposalFlow constructor(
    private val otherSideSession: FlowSession,
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return otherSideSession.receive<SignedTransaction>().unwrap {
            checkParameterHash(it.tx.networkParametersHash)

            // Resolve all utxo dependencies
            subFlow(ZKReceiveUtxoInfoFlow(otherSideSession))

            // Download missing attachments
            fetchMissingAttachments(it.tx, otherSideSession)

            // Verify the transaction. We don't know if we will have all sigs, since this is only a proposal, not final
            it.zkVerify(serviceHub, checkSufficientSignatures = false)

            // Verify that we have a validated backchain stored for this transaction
            serviceHub.getCordaServiceFromConfig<ZKTransactionService>(ServiceNames.ZK_TX_SERVICE)
                .validateBackchain(it.tx)

            it
        }
    }
}

/**
 * The [ZkReceiveUtxoInfoFlow] should be called in response to the [SendUtxoInfosFlow].
 *
 * This flow is a combination of [FlowSession.receive] and resolve. This flow will receive a list of [UtxoInfo]
 * and perform the resolution back-and-forth required to check the dependencies.
 * The flow will return the list of [UtxoInfo] after it is resolved.
 */
// @JvmSuppressWildcards is used to suppress wildcards in return type when calling `subFlow(new ReceiveUtxoInfo<T>(otherParty))` in java.
class ZKReceiveUtxoInfoFlow(private val otherSideSession: FlowSession) :
    FlowLogic<@JvmSuppressWildcards List<UtxoInfo>>() {
    @Suspendable
    override fun call(): List<UtxoInfo> {
        // 1. Receive the list of UtxoInfo that the counterparty sent us
        val utxoInfos = otherSideSession.receive<List<UtxoInfo>>().unwrap { it }
        val txHashes = utxoInfos.asSequence().map { it.stateRef.txhash }.toSet()

        // 2. Resolve the ZKP chain for each UtxoInfo
        subFlow(ResolveZKTransactionsFlow(null, txHashes, otherSideSession))

        utxoInfos.forEach { utxoInfo ->
            // 3. Verify Utxo
            utxoInfo.verify(serviceHub)

            // 4. Store the UtxoInfo in, so we can fetch it together with its ZKP chain when we need it elsewhere
            serviceHub.getCordaServiceFromConfig<WritableUtxoInfoStorage>(ServiceNames.ZK_UTXO_INFO_STORAGE)
                .addUtxoInfo(utxoInfo)
        }
        return utxoInfos
    }
}

/**
 * The [ZkReceiveStateAndRefFlow] should be called in response to the [ZKSendStateAndRefFlow].
 *
 * This flow is a combination of [FlowSession.receive] and resolve. This flow will receive a list of [StateAndRef]
 * and perform the resolution back-and-forth required to check the dependencies.
 * The flow will return the list of [StateAndRef] after it is resolved.
 */
// @JvmSuppressWildcards is used to suppress wildcards in return type when calling `subFlow(new ReceiveStateAndRef<T>(otherParty))` in java.
class ZKReceiveStateAndRefFlow<out T : ContractState>(private val otherSideSession: FlowSession) :
    FlowLogic<@JvmSuppressWildcards List<StateAndRef<T>>>() {
    @Suspendable
    override fun call(): List<StateAndRef<T>> {
        // 1. Receive the list of UtxoInfo that the counterparty sent us
        val utxoInfos = subFlow(ZKReceiveUtxoInfoFlow(otherSideSession))

        return utxoInfos.map { utxoInfo ->
            // 2. Return it as a StateAndRef
            StateAndRef(
                utxoInfo.serializedContents.deserialize(),
                utxoInfo.stateRef
            )
        }
    }
}
