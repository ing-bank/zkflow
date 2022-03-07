package com.ing.zkflow.testing.dsl.interfaces

import net.corda.core.DoNotImplement
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import java.security.PublicKey

/**
 * This interface defines the bare bone public functionality that a Transaction DSL interpreter should implement.
 * @param <R> The return type of [verifies]/[failsWith] and the like. It is generic so that we have control over whether
 * we want to enforce users to call these methods (see [EnforceVerifyOrFail]) or not.
 */
@DoNotImplement
@Suppress("FunctionNaming") // Copy of Corda API
public interface ZKTransactionDSLInterpreter : Verifies, OutputStateLookup {
    /**
     * A reference to the enclosing ledger{..}'s interpreter.
     */
    public val ledgerInterpreter: ZKLedgerDSLInterpreter<ZKTransactionDSLInterpreter>

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
    @Suppress("LongParameterList") // Copy of Corda API
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
     * @param attachmentId The hash of the attachment, possibly returned by [ZKLedgerDSLInterpreter.attachment].
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
    public fun _tweak(dsl: ZKTransactionDSLInterpreter.() -> EnforceVerifyOrFail): EnforceVerifyOrFail

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
    public fun _attachment(
        contractClassName: ContractClassName,
        attachmentId: AttachmentId,
        signers: List<PublicKey>,
        jarManifestAttributes: Map<String, String>
    )
}
