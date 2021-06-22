@file:Suppress("FunctionName", "FunctionNaming", "FunctionParameterNaming", "LongParameterList", "TooManyFunctions") // Copy of Corda API

package com.ing.zknotary.testing.dsl

import net.corda.core.DoNotImplement
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.seconds
import java.security.PublicKey
import java.time.Duration
import java.time.Instant

/**
 * This interface defines the bare bone public functionality that a Transaction DSL interpreter should implement.
 * @param <R> The return type of [verifies]/[failsWith] and the like. It is generic so that we have control over whether
 * we want to enforce users to call these methods (see [EnforceVerifyOrFail]) or not.
 */
@DoNotImplement
public interface TransactionDSLInterpreter : Verifies, OutputStateLookup {
    /**
     * A reference to the enclosing ledger{..}'s interpreter.
     */
    public val ledgerInterpreter: LedgerDSLInterpreter<TransactionDSLInterpreter>

    /**
     * Adds an input reference to the transaction. Note that [verifies] will resolve this reference.
     * @param stateRef The input [StateRef].
     */
    public fun input(stateRef: StateRef)

    /**
     * Add a reference input state to the transaction. Note that [verifies] will resolve this reference.
     * @param stateRef The input [StateRef].
     */
    public fun reference(stateRef: StateRef)

    /**
     * Adds an output to the transaction.
     * @param label An optional label that may be later used to retrieve the output probably in other transactions.
     * @param notary The associated notary.
     * @param encumbrance The position of the encumbrance state.
     * @param attachmentConstraint The attachment constraint
     * @param contractState The state itself.
     * @param contractClassName The class name of the contract that verifies this state.
     */
    public fun output(
        contractClassName: ContractClassName,
        label: String?,
        notary: Party,
        encumbrance: Int?,
        attachmentConstraint: AttachmentConstraint,
        contractState: ContractState
    )

    /**
     * Adds an [Attachment] reference to the transaction.
     * @param attachmentId The hash of the attachment, possibly returned by [LedgerDSLInterpreter.attachment].
     */
    public fun attachment(attachmentId: SecureHash)

    /**
     * Adds a command to the transaction.
     * @param signers The signer public keys.
     * @param commandData The contents of the command.
     */
    public fun command(signers: List<PublicKey>, commandData: CommandData)

    /**
     * Sets the time-window of the transaction.
     * @param data the [TimeWindow] (validation window).
     */
    public fun timeWindow(data: TimeWindow)

    /**
     * Creates a local scoped copy of the transaction.
     * @param dsl The transaction DSL to be interpreted using the copy.
     */
    public fun _tweak(dsl: TransactionDSLInterpreter.() -> EnforceVerifyOrFail): EnforceVerifyOrFail

    /**
     * Attaches an attachment containing the named contract to the transaction
     * @param contractClassName The contract class to attach
     */
    public fun _attachment(contractClassName: ContractClassName)

    /**
     * Attaches an attachment containing the named contract to the transaction
     * @param contractClassName The contract class to attach
     * @param attachmentId The attachment
     */
    public fun _attachment(contractClassName: ContractClassName, attachmentId: AttachmentId, signers: List<PublicKey>)

    /**
     * Attaches an attachment containing the named contract to the transaction.
     * @param contractClassName The contract class to attach.
     * @param attachmentId The attachment.
     * @param signers The signers.
     * @param jarManifestAttributes The JAR manifest file attributes.
     */
    public fun _attachment(contractClassName: ContractClassName, attachmentId: AttachmentId, signers: List<PublicKey>, jarManifestAttributes: Map<String, String>)
}

/**
 * Underlying class for the transaction DSL. Do not instantiate directly, instead use the [transaction] public function.
 * */
public class TransactionDSL<out T : TransactionDSLInterpreter>(interpreter: T, private val notary: Party) : TransactionDSLInterpreter by interpreter {
    /**
     * Looks up the output label and adds the found state as an reference input state.
     * @param stateLabel The label of the output state specified when calling [TransactionDSLInterpreter.output] and friends.
     */
    public fun reference(stateLabel: String): Unit = reference(retrieveOutputStateAndRef(ContractState::class.java, stateLabel).ref)

    /**
     * Creates an [LedgerDSLInterpreter._unverifiedTransaction] with a single reference input state and adds its
     * reference as in input to the current transaction.
     * @param state The state to be added.
     */
    public fun reference(contractClassName: ContractClassName, state: ContractState) {
        val transaction = ledgerInterpreter._unverifiedTransaction(null, TransactionBuilder(notary)) {
            output(contractClassName, null, notary, null, AlwaysAcceptAttachmentConstraint, state)
        }
        reference(transaction.outRef<ContractState>(0).ref)
    }

    /**
     * Looks up the output label and adds the found state as an input.
     * @param stateLabel The label of the output state specified when calling [TransactionDSLInterpreter.output] and friends.
     */
    public fun input(stateLabel: String): Unit = input(retrieveOutputStateAndRef(ContractState::class.java, stateLabel).ref)

    public fun input(contractClassName: ContractClassName, stateLabel: String) {
        val stateAndRef = retrieveOutputStateAndRef(ContractState::class.java, stateLabel)
        input(contractClassName, stateAndRef.state.data)
    }

    /**
     * Creates an [LedgerDSLInterpreter._unverifiedTransaction] with a single output state and adds its reference as an
     * input to the current transaction.
     * @param state The state to be added.
     */
    public fun input(contractClassName: ContractClassName, state: ContractState) {
        val transaction = ledgerInterpreter._unverifiedTransaction(null, TransactionBuilder(notary)) {
            output(contractClassName, null, notary, null, AlwaysAcceptAttachmentConstraint, state)
        }
        input(transaction.outRef<ContractState>(0).ref)
    }

    /**
     * Adds a labelled output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, label: String, notary: Party, contractState: ContractState): Unit =
        output(contractClassName, label, notary, null, AutomaticPlaceholderConstraint, contractState)

    /**
     * Adds a labelled output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, label: String, encumbrance: Int, contractState: ContractState): Unit =
        output(contractClassName, label, notary, encumbrance, AutomaticPlaceholderConstraint, contractState)

    /**
     * Adds a labelled output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, label: String, contractState: ContractState): Unit =
        output(contractClassName, label, notary, null, AutomaticPlaceholderConstraint, contractState)

    /**
     * Adds an output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, notary: Party, contractState: ContractState): Unit =
        output(contractClassName, null, notary, null, AutomaticPlaceholderConstraint, contractState)

    /**
     * Adds an output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, encumbrance: Int, contractState: ContractState): Unit =
        output(contractClassName, null, notary, encumbrance, AutomaticPlaceholderConstraint, contractState)

    /**
     * Adds an output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, contractState: ContractState): Unit =
        output(contractClassName, null, notary, null, AutomaticPlaceholderConstraint, contractState)

    /**
     * Adds a command to the transaction.
     */
    public fun command(signer: PublicKey, commandData: CommandData): Unit = command(listOf(signer), commandData)

    /**
     * Sets the [TimeWindow] of the transaction.
     * @param time The [Instant] of the [TimeWindow].
     * @param tolerance The tolerance of the [TimeWindow].
     */
    @JvmOverloads
    public fun timeWindow(time: Instant, tolerance: Duration = 30.seconds): Unit =
        timeWindow(TimeWindow.withTolerance(time, tolerance))

    /** Creates a local scoped copy of the transaction. */
    public fun tweak(dsl: TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail): EnforceVerifyOrFail =
        _tweak { TransactionDSL(this, notary).dsl() }

    /**
     * @see TransactionDSLInterpreter._attachment
     */
    public fun attachment(contractClassName: ContractClassName): Unit = _attachment(contractClassName)

    public fun attachment(contractClassName: ContractClassName, attachmentId: AttachmentId, signers: List<PublicKey>, jarManifestAttributes: Map<String, String> = emptyMap()): Unit = _attachment(contractClassName, attachmentId, signers, jarManifestAttributes)
    public fun attachment(contractClassName: ContractClassName, attachmentId: AttachmentId): Unit = _attachment(contractClassName, attachmentId, emptyList())

    public fun attachments(vararg contractClassNames: ContractClassName): Unit = contractClassNames.forEach { attachment(it) }
}
