package com.ing.zknotary.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.crypto.isFulfilledBy
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.NotaryException
import net.corda.core.flows.NotaryFlow
import net.corda.core.flows.SendTransactionFlow
import net.corda.core.flows.UnexpectedFlowEndException
import net.corda.core.identity.Party
import net.corda.core.identity.groupAbstractPartyByWellKnownParty
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

/**
 * Verifies the given transaction, then sends it to the named notary. If the notary agrees that the transaction
 * is acceptable then it is from that point onwards committed to the ledger, and will be written through to the
 * vault. Additionally it will be distributed to the parties reflected in the participants list of the states.
 *
 * The transaction is expected to have already been resolved: if its dependencies are not available in local
 * storage, verification will fail. It must have signatures from all necessary parties other than the notary.
 *
 * A list of [FlowSession]s is required for each non-local participant of the transaction. These participants will receive
 * the final notarised transaction by calling [ReceiveFinalityFlow] in their counterpart com.ing.zknotary.flows. Sessions with non-participants
 * can also be included, but they must specify [StatesToRecord.ALL_VISIBLE] for statesToRecord if they wish to record the
 * contract states into their vaults.
 *
 * The flow returns the same transaction but with the additional signatures from the notary.
 *
 * NOTE: This is an inlined flow but for backwards compatibility is annotated with [InitiatingFlow].
 */
// To maintain backwards compatibility with the old API, FinalityFlow can act both as an initiating flow and as an inlined flow.
// This is only possible because a flow is only truly initiating when the first call to initiateFlow is made (where the
// presence of @InitiatingFlow is checked). So the new API is inlined simply because that code path doesn't call initiateFlow.
@InitiatingFlow
class ZKFinalityFlow private constructor(
    val transaction: SignedTransaction,
    val zkTransaction: SignedZKVerifierTransaction,
    override val progressTracker: ProgressTracker,
    private val sessions: Collection<FlowSession>
) : FlowLogic<SignedTransaction>() {

    /**
     * Notarise the given transaction and broadcast it to all the participants.
     *
     * @param transaction What to commit.
     * @param sessions A collection of [FlowSession]s for each non-local participant of the transaction. Sessions to non-participants can
     * also be provided.
     */
    @JvmOverloads
    constructor(
        transaction: SignedTransaction,
        zkTransaction: SignedZKVerifierTransaction,
        sessions: Collection<FlowSession>,
        progressTracker: ProgressTracker = tracker()
    ) : this(transaction, zkTransaction, progressTracker, sessions)

    companion object {
        object NOTARISING : ProgressTracker.Step("Requesting signature by notary service") {
            override fun childProgressTracker() = NotaryFlow.Client.tracker()
        }

        object BROADCASTING : ProgressTracker.Step("Broadcasting transaction to participants")

        @JvmStatic
        fun tracker() = ProgressTracker(
            NOTARISING,
            BROADCASTING
        )
    }

    @Suspendable
    @Throws(NotaryException::class)
    override fun call(): SignedTransaction {
        require(sessions.none { serviceHub.myInfo.isLegalIdentity(it.counterparty) }) {
            "Do not provide flow sessions for the local node. ZKFinalityFlow will record the notarised transaction locally."
        }

        // Note: this method is carefully broken up to minimize the amount of data reachable from the stack at
        // the point where subFlow is invoked, as that minimizes the checkpointing work to be done.
        //
        // Lookup the resolved transactions and use them to map each signed transaction to the list of participants.
        // Then send to the notary if needed, record locally and distribute.

        logCommandData()
        val ledgerTransaction = verifyTx()
        val externalTxParticipants = extractExternalParticipants(ledgerTransaction)

        val sessionParties = sessions.map { it.counterparty }
        val missingRecipients = externalTxParticipants - sessionParties
        require(missingRecipients.isEmpty()) {
            "Flow sessions were not provided for the following transaction participants: $missingRecipients"
        }

        val notarised = notariseAndRecord()

        progressTracker.currentStep =
            BROADCASTING

        for (session in sessions) {
            try {
                subFlow(SendTransactionFlow(session, notarised))
                logger.info("Party ${session.counterparty} received the transaction.")
            } catch (e: UnexpectedFlowEndException) {
                throw UnexpectedFlowEndException(
                    "${session.counterparty} has finished prematurely and we're trying to send them the finalised transaction. " +
                        "Did they forget to call ReceiveFinalityFlow? (${e.message})",
                    e.cause,
                    e.originalErrorId
                )
            }
        }

        logger.info("All parties received the transaction successfully.")

        return notarised
    }

    private fun logCommandData() {
        if (logger.isDebugEnabled) {
            val commandDataTypes =
                transaction.tx.commands.asSequence().mapNotNull { it.value::class.qualifiedName }.distinct()
            logger.debug("Started finalization, commands are ${commandDataTypes.joinToString(", ", "[", "]")}.")
        }
    }

    @Suspendable
    private fun notariseAndRecord(): SignedTransaction {
        val notarised = if (needsNotarySignature(transaction)) {
            progressTracker.currentStep =
                NOTARISING
            val notarySignatures = subFlow(ZKNotaryFlow(transaction, TODO()))
            transaction + notarySignatures
        } else {
            logger.info("No need to notarise this transaction.")
            transaction
        }
        logger.info("Recording transaction locally.")
        recordTransactions(notarised, zkTransaction)
        logger.info("Recorded transaction locally successfully.")
        return notarised
    }

    private fun recordTransactions(notarised: SignedTransaction, zkTransaction: SignedZKVerifierTransaction) {

        // Record plaintext transaction
        serviceHub.recordTransactions(notarised)

        // Record ZK transaction
        val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage =
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)
        zkVerifierTransactionStorage.map.put(notarised, zkTransaction.tx)
        zkVerifierTransactionStorage.addTransaction(zkTransaction)
    }

    private fun needsNotarySignature(stx: SignedTransaction): Boolean {
        val wtx = stx.tx
        val needsNotarisation = wtx.inputs.isNotEmpty() || wtx.references.isNotEmpty() || wtx.timeWindow != null
        return needsNotarisation && hasNoNotarySignature(stx)
    }

    private fun hasNoNotarySignature(stx: SignedTransaction): Boolean {
        val notaryKey = stx.tx.notary?.owningKey
        val signers = stx.sigs.asSequence().map { it.by }.toSet()
        return notaryKey?.isFulfilledBy(signers) != true
    }

    private fun extractExternalParticipants(ltx: LedgerTransaction): Set<Party> {
        val participants = ltx.outputStates.flatMap { it.participants } + ltx.inputStates.flatMap { it.participants }
        return groupAbstractPartyByWellKnownParty(serviceHub, participants).keys - serviceHub.myInfo.legalIdentities
    }

    // For this first version, we still resolve the entire plaintext history of the transaction
    private fun verifyTx(): LedgerTransaction {
        val notary = transaction.tx.notary
        // The notary signature(s) are allowed to be missing but no others.
        if (notary != null) transaction.verifySignaturesExcept(notary.owningKey) else transaction.verifyRequiredSignatures()
        val ltx = transaction.toLedgerTransaction(serviceHub, false)
        ltx.verify()
        return ltx
    }
}
