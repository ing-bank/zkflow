package com.ing.zkflow.testing.dsl

import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.testing.dsl.interfaces.EnforceVerifyOrFail
import com.ing.zkflow.testing.dsl.interfaces.ZKTransactionDSLInterpreter
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TimeWindow
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.utilities.seconds
import java.security.PublicKey
import java.time.Duration
import java.time.Instant

/**
 * Underlying class for the transaction DSL. Do not instantiate directly, instead use the [transaction] public function.
 * */
@Suppress("TooManyFunctions")
public class ZKTransactionDSL<out T : ZKTransactionDSLInterpreter>(private val interpreter: T, private val notary: Party) :
    ZKTransactionDSLInterpreter by interpreter {
    /**
     * Looks up the output label and adds the found state as an reference input state.
     * @param stateLabel The label of the output state specified when calling [ZKTransactionDSLInterpreter.output] and friends.
     */
    public fun reference(stateLabel: String): Unit = reference(retrieveOutputStateAndRef(ContractState::class.java, stateLabel).ref)

    /**
     * Creates an [ZKLedgerDSLInterpreter._unverifiedTransaction] with a single reference input state and adds its
     * reference as in input to the current transaction.
     * @param state The state to be added.
     */
    public fun reference(contractClassName: ContractClassName, state: ContractState) {
        val transaction = ledgerInterpreter._unverifiedTransaction(
            null,
            ZKTransactionBuilder(notary, interpreter.ledgerInterpreter.zkNetworkParameters)
        ) {
            output(contractClassName, null, notary, null, AlwaysAcceptAttachmentConstraint, state)
        }
        reference(transaction.outRef<ContractState>(0).ref)
    }

    /**
     * Looks up the output label and adds the found state as an input.
     * @param stateLabel The label of the output state specified when calling [ZKTransactionDSLInterpreter.output] and friends.
     */
    public fun input(stateLabel: String): Unit = input(retrieveOutputStateAndRef(ContractState::class.java, stateLabel).ref)

    public fun input(contractClassName: ContractClassName, stateLabel: String) {
        val stateAndRef = retrieveOutputStateAndRef(ContractState::class.java, stateLabel)
        input(contractClassName, stateAndRef.state.data)
    }

    /**
     * Creates an [ZKLedgerDSLInterpreter._unverifiedTransaction] with a single output state and adds its reference as an
     * input to the current transaction.
     * @param state The state to be added.
     */
    public fun input(contractClassName: ContractClassName, state: ContractState) {
        val transaction = ledgerInterpreter._unverifiedTransaction(
            null,
            ZKTransactionBuilder(notary, interpreter.ledgerInterpreter.zkNetworkParameters)
        ) {
            output(contractClassName, null, notary, null, AlwaysAcceptAttachmentConstraint, state)
        }
        input(transaction.outRef<ContractState>(0).ref)
    }

    /**
     * Adds a labelled output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, label: String, notary: Party, contractState: ContractState): Unit =
        output(contractClassName, label, notary, null, AlwaysAcceptAttachmentConstraint, contractState)

    /**
     * Adds a labelled output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, label: String, encumbrance: Int, contractState: ContractState): Unit =
        output(contractClassName, label, notary, encumbrance, AlwaysAcceptAttachmentConstraint, contractState)

    /**
     * Adds a labelled output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, label: String, contractState: ContractState): Unit =
        output(contractClassName, label, notary, null, AlwaysAcceptAttachmentConstraint, contractState)

    /**
     * Adds an output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, notary: Party, contractState: ContractState): Unit =
        output(contractClassName, null, notary, null, AlwaysAcceptAttachmentConstraint, contractState)

    /**
     * Adds an output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, encumbrance: Int, contractState: ContractState): Unit =
        output(contractClassName, null, notary, encumbrance, AlwaysAcceptAttachmentConstraint, contractState)

    /**
     * Adds an output to the transaction.
     */
    public fun output(contractClassName: ContractClassName, contractState: ContractState): Unit =
        output(contractClassName, null, notary, null, AlwaysAcceptAttachmentConstraint, contractState)

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
    public fun tweak(dsl: ZKTransactionDSL<ZKTransactionDSLInterpreter>.() -> EnforceVerifyOrFail): EnforceVerifyOrFail =
        _tweak { ZKTransactionDSL(this, notary).dsl() }

    /**
     * @see ZKTransactionDSLInterpreter._attachment
     */
    public fun attachment(contractClassName: ContractClassName): Unit = _attachment(contractClassName)

    public fun attachment(contractClassName: ContractClassName, attachmentId: AttachmentId, signers: List<PublicKey>, jarManifestAttributes: Map<String, String> = emptyMap()): Unit = _attachment(contractClassName, attachmentId, signers, jarManifestAttributes)
    public fun attachment(contractClassName: ContractClassName, attachmentId: AttachmentId): Unit = _attachment(contractClassName, attachmentId, emptyList())

    public fun attachments(vararg contractClassNames: ContractClassName): Unit = contractClassNames.forEach { attachment(it) }
}
