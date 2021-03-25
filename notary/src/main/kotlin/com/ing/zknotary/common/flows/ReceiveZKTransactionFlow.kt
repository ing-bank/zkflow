package com.ing.zknotary.common.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.recordTransactions
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.UtxoInfo
import com.ing.zknotary.common.transactions.dependencies
import com.ing.zknotary.common.transactions.zkVerify
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.WritableUtxoInfoStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.internal.checkParameterHash
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.trace
import net.corda.core.utilities.unwrap
import java.security.SignatureException

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
 */
open class ZKReceiveZKTransactionFlow @JvmOverloads constructor(
    private val stx: SignedTransaction,
    private val otherSideSession: FlowSession,
    private val checkSufficientSignatures: Boolean = true,
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
        val zkService = serviceHub.getCordaServiceFromConfig<ZKTransactionService>(ServiceNames.ZK_TX_SERVICE)
        val svtx = otherSideSession.receive<SignedZKVerifierTransaction>().unwrap {
            logger.info("Received transaction acknowledgement request from party ${otherSideSession.counterparty}.")
            checkParameterHash(it.tx.networkParametersHash)
            subFlow(ResolveZKTransactionsFlow(stx, it.dependencies, otherSideSession))
            logger.info("Transaction dependencies resolution completed.")
            require(stx.id == it.id) { "IDs of STX and SVTX should match" }
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
            // Here sometimes (e.g. in ReceiveFinalityflow) we receive VTX that contains full set of sigs,
            // while STX lacks some (e.g. Notary's). In this case we take sigs from VTX and save STX with full set.
            // Not super pretty solution, can be refactored to something better later.
            serviceHub.recordTransactions(SignedTransaction(stx.txBits, svtx.sigs), svtx)
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
    protected open fun checkBeforeRecording(svtx: SignedZKVerifierTransaction) {
    }
}

/**
 * This is the counterpart to [ZKSendTransactionProposal].
 *
 * It should *only* be used for receiving transaction proposals, not for receiving finalized transactions.
 *
 * Use [ZKReceiveZKTransactionFlow] and its counterpart [SendZKTransactionFlow] for sending finalized
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

            // Resolve all dependencies
            subFlow(ZKReceiveUtxoInfoFlow(otherSideSession))

            // Verify the transaction. We don't know if we will have all sigs, since this is only a proposal, not final
            it.zkVerify(serviceHub, false)

            // Verify its ZKP backchain, including proof verification
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
