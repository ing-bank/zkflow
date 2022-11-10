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
import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.transactions.NotarisedTransactionPayload
import com.ing.zkflow.common.transactions.PrivateNotarisedTransactionPayload
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.resolvePublicOrPrivateStateRef
import com.ing.zkflow.common.transactions.verification.prove
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.isFulfilledBy
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.NotaryException
import net.corda.core.flows.NotaryFlow
import net.corda.core.flows.UnexpectedFlowEndException
import net.corda.core.identity.Party
import net.corda.core.identity.groupAbstractPartyByWellKnownParty
import net.corda.core.internal.SerializedStateAndRef
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
class ZKFinalityFlow private constructor(
    val stx: SignedTransaction,
    override val progressTracker: ProgressTracker,
    /**
     * The sessions that will receive the full transaction data, including all private data.
     */
    private val privateSessions: Collection<FlowSession>,
    /**
     * The sessions that will receive only the public transaction data
     */
    private val publicSessions: Collection<FlowSession>,
) : FlowLogic<SignedTransaction>() {

    /**
     * Notarise the given transaction and broadcast it to all the participants.
     *
     * @param transaction What to commit.
     * @param publicSessions A collection of [FlowSession]s for each non-local participant of the transaction. Sessions to non-participants can
     * also be provided.
     */
    @JvmOverloads
    constructor(
        transaction: SignedTransaction,
        privateSessions: Collection<FlowSession>,
        publicSessions: Collection<FlowSession> = emptyList(),
        progressTracker: ProgressTracker = tracker()
    ) : this(transaction, progressTracker, privateSessions = privateSessions, publicSessions = publicSessions)

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
        // Note: this method is carefully broken up to minimize the amount of data reachable from the stack at
        // the point where subFlow is invoked, as that minimizes the checkpointing work to be done.
        //
        // Lookup the resolved transactions and use them to map each signed transaction to the list of participants.
        // Then send to the notary if needed, record locally and distribute.

        logCommandData()

        val sessionParties = (publicSessions + privateSessions).map { it.counterparty }.toSet()
        require(sessionParties.none { serviceHub.myInfo.isLegalIdentity(it) }) {
            "Do not provide flow sessions for the local node. ZKFinalityFlow will record the notarised transaction locally." +
                "\nLocal node identities: ${serviceHub.myInfo.legalIdentities}, " +
                "\nPrivate SessionParties: ${privateSessions.map { it.counterparty }} " +
                "\nPublic SessionParties: ${publicSessions.map { it.counterparty }} "
        }

        val externalTxParticipants = extractExternalParticipants(stx.tx)
        val missingRecipients = externalTxParticipants - sessionParties
        require(missingRecipients.isEmpty()) {
            "Flow sessions were not provided for the following transaction participants: $missingRecipients"
        }

        val notarised = notariseAndRecord()

        progressTracker.currentStep =
            BROADCASTING

        // We send the full `PrivateNotarisedTransactionPayload` to the private sessions
        for (session in privateSessions) {
            try {
                subFlow(SendNotarisedTransactionPayloadFlow(session, notarised))
                logger.info("Party ${session.counterparty} received the full SignedTransaction and the private ZKVerifierTransaction.")
            } catch (e: UnexpectedFlowEndException) {
                throw UnexpectedFlowEndException(
                    "${session.counterparty} has finished prematurely and we're trying to send them the finalised transaction. " +
                        "Did they forget to call ZKReceiveFinalityFlow? (${e.message})",
                    e.cause,
                    e.originalErrorId
                )
            }
        }

        // We send only the public `PublicNotarisedTransactionPayload` to the public sessions
        for (session in publicSessions) {
            try {
                subFlow(SendNotarisedTransactionPayloadFlow(session, notarised.toPublic()))
                logger.info("Party ${session.counterparty} received the private ZKVerifierTransaction.")
            } catch (e: UnexpectedFlowEndException) {
                throw UnexpectedFlowEndException(
                    "${session.counterparty} has finished prematurely and we're trying to send them the finalised transaction. " +
                        "Did they forget to call ZKReceiveFinalityFlow? (${e.message})",
                    e.cause,
                    e.originalErrorId
                )
            }
        }

        logger.info("All parties received the transaction successfully.")

        return notarised.stx
    }

    private fun logCommandData() {
        if (logger.isDebugEnabled) {
            val commandDataTypes =
                stx.tx.commands.asSequence().mapNotNull { it.value::class.qualifiedName }.distinct()
            logger.debug("Started finalization, commands are ${commandDataTypes.joinToString(", ", "[", "]")}.")
        }
    }

    @Suspendable
    private fun notariseAndRecord(): PrivateNotarisedTransactionPayload {
        // Create proof and svtx
        val svtx = stx.prove(serviceHub)

        val notarisedSvtx = if (needsNotarySignature(svtx)) {
            progressTracker.currentStep =
                NOTARISING
            val notarySignatures = subFlow(ZKNotaryFlow(stx, svtx))
            svtx + notarySignatures
        } else {
            logger.info("No need to notarise this transaction.")
            svtx
        }
        logger.info("Recording transaction locally.")
        val notarised = PrivateNotarisedTransactionPayload(notarisedSvtx, SignedTransaction(stx.tx, notarisedSvtx.sigs))
        serviceHub.recordTransactions(notarised)
        logger.info("Recorded transaction locally successfully.")
        return notarised
    }

    private fun needsNotarySignature(svtx: SignedZKVerifierTransaction): Boolean {
        val wtx = svtx.tx
        val needsNotarisation = wtx.inputs.isNotEmpty() || wtx.references.isNotEmpty() || wtx.timeWindow != null
        return needsNotarisation && hasNoNotarySignature(svtx)
    }

    private fun hasNoNotarySignature(svtx: SignedZKVerifierTransaction): Boolean {
        val notaryKey = svtx.tx.notary?.owningKey
        val signers = svtx.sigs.asSequence().map { it.by }.toSet()
        return notaryKey?.isFulfilledBy(signers) != true
    }

    private fun extractExternalParticipants(tx: TraversableTransaction): Set<Party> {
        val inputStates = tx.inputs.map {
            SerializedStateAndRef(
                resolvePublicOrPrivateStateRef(
                    it,
                    serviceHub,
                    serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE),
                    serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_UTXO_INFO_STORAGE)
                ),
                it
            ).toStateAndRef().state.data
        }
        val participants = tx.outputStates.flatMap { it.participants } + inputStates.flatMap { it.participants }
        return groupAbstractPartyByWellKnownParty(serviceHub, participants).keys - serviceHub.myInfo.legalIdentities
    }
}

/**
 * The receiving counterpart to [FinalityFlow].
 *
 * All parties who are receiving a finalised transaction from a sender flow must subcall this flow in their own flows.
 *
 * It's typical to have already signed the transaction proposal in the same workflow using [ZKSignTransactionFlow]. If so
 * then the transaction ID can be passed in as an extra check to ensure the finalised transaction is the one that was signed
 * before it's committed to the vault.
 *
 * @param otherSideSession The session which is providing the transaction to record.
 * @param statesToRecord Which states to commit to the vault. Defaults to [StatesToRecord.ONLY_RELEVANT].
 */
class ZKReceiveFinalityFlow @JvmOverloads constructor(
    private val otherSideSession: FlowSession,
    private val expectedTxId: SecureHash? = null,
    private val statesToRecord: StatesToRecord = StatesToRecord.ONLY_RELEVANT
) : FlowLogic<SignedZKVerifierTransaction>() {
    @Suspendable
    override fun call(): SignedZKVerifierTransaction {
        return subFlow(object : ZKReceiveNotarisedTransactionPayloadFlow(
            otherSideSession,
            checkSufficientSignatures = true,
            statesToRecord = statesToRecord
        ) {
                override fun checkBeforeRecording(notarised: NotarisedTransactionPayload) {
                    expectedTxId?.let {
                        require(it == notarised.svtx.id) {
                            "We expected to receive transaction with ID $it but instead got ${notarised.svtx.id}. Transaction was" +
                                "not recorded, nor its states sent to the vault."
                        }
                    }
                }
            })
    }
}
