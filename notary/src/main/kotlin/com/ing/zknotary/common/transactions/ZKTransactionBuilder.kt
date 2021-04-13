package com.ing.zknotary.common.transactions

import co.paralleluniverse.strands.Strand
import com.ing.zknotary.common.serialization.bfl.FixedLengthSerializationScheme
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.identity.Party
import net.corda.core.internal.FlowStateMachine
import net.corda.core.internal.requiredContractClassName
import net.corda.core.node.ServiceHub
import net.corda.core.node.ServicesForResolution
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.KeyManagementService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import java.security.PublicKey
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * The main reason for this ZKTransactionBuilder to exist, is to ensure that the user always uses the
 * serialization scheme required for ZKP and to make the experience as seamless as possible.
 *
 * Without those considerations, this class would not be necessary.
 *
 * Unfortunately, to achieve the seamless experience, we need to either extend the TransactionBuilder,
 * which requires a *lot* of copy-paste and is hard to maintain functional parity with the original,
 * or decorate it as we have done here.
 *
 * TODO: Ask R3 to create a TransactionBuilder interface:
 * Ideally, there would be a TransactionBuilder interface, so that we could do the following to get free delegations
 * for all functions and we would only have to override what we need:
 *
 * ```
 * class ZKTransactionBuilder(builder: TxBuilderIface): TxBuilderIface by builder
 * ```
 */
@Suppress("TooManyFunctions", "LongParameterList", "MemberVisibilityCanBePrivate") // Copy of TransactionBuilder API
class ZKTransactionBuilder(
    private val builder: TransactionBuilder,
    private val serializationSchemeId: Int = FixedLengthSerializationScheme.SCHEME_ID,
    private val serializationProperties: Map<Any, Any> = emptyMap()
) {

    constructor(
        notary: Party? = null,
        lockId: UUID = defaultLockId(),
        inputs: MutableList<StateRef> = arrayListOf(),
        attachments: MutableList<AttachmentId> = arrayListOf(),
        outputs: MutableList<TransactionState<ContractState>> = arrayListOf(),
        commands: MutableList<Command<*>> = arrayListOf(),
        window: TimeWindow? = null,
        privacySalt: PrivacySalt = PrivacySalt(),
        references: MutableList<StateRef> = arrayListOf(),
        serviceHub: ServiceHub? = (Strand.currentStrand() as? FlowStateMachine<*>)?.serviceHub

    ) : this(
        TransactionBuilder(
            notary,
            lockId,
            inputs,
            attachments,
            outputs,
            commands,
            window,
            privacySalt,
            references,
            serviceHub
        )
    )

    constructor(
        notary: Party? = null,
        lockId: UUID = defaultLockId(),
        inputs: MutableList<StateRef> = arrayListOf(),
        attachments: MutableList<AttachmentId> = arrayListOf(),
        outputs: MutableList<TransactionState<ContractState>> = arrayListOf(),
        commands: MutableList<Command<*>> = arrayListOf(),
        window: TimeWindow? = null,
        privacySalt: PrivacySalt = PrivacySalt()
    ) : this(notary, lockId, inputs, attachments, outputs, commands, window, privacySalt, arrayListOf())

    constructor(notary: Party) : this(notary, window = null)

    private companion object {
        // Copied from private `TransactionBuilder.defaultLockId`
        private fun defaultLockId() = (Strand.currentStrand() as? FlowStateMachine<*>)?.id?.uuid ?: UUID.randomUUID()
    }

    /*
     * START: copy of [TransactionBuilder] API with changed behaviour
     */

    /**
     * Duplicated so that `toWireTransaction()` always uses the serialization settings
     */
    fun toWireTransaction(services: ServicesForResolution): WireTransaction =
        builder.toWireTransaction(services, serializationSchemeId, serializationProperties)

    /**
     * Duplicated so that it uses our custom `toWireTransaction()` function
     */
    fun toSignedTransaction(
        keyManagementService: KeyManagementService,
        publicKey: PublicKey,
        signatureMetadata: SignatureMetadata,
        services: ServicesForResolution
    ): SignedTransaction {
        val wtx = toWireTransaction(services)
        val signableData = SignableData(wtx.id, signatureMetadata)
        val sig = keyManagementService.sign(signableData, publicKey)
        return SignedTransaction(wtx, listOf(sig))
    }

    /**
     * Duplicated so that it uses our custom `toWireTransaction()` function
     */
    fun toLedgerTransaction(services: ServiceHub) = toWireTransaction(services).toLedgerTransaction(services)

    fun verify(services: ServiceHub) = toLedgerTransaction(services).verify()

    fun copy(): ZKTransactionBuilder = ZKTransactionBuilder(builder.copy(), serializationSchemeId)
    /*
     * END: copy of [TransactionBuilder] API with changed behaviour
     */

    /*
     * START: copy of [TransactionBuilder] API that delegates unchanged
     */

    var notary: Party?
        get() = builder.notary
        set(value) {
            builder.notary = value
        }

    var lockId: UUID
        get() = builder.lockId
        set(value) {
            builder.lockId = value
        }

    /**
     * This function is purposely disabled and only present for API compatibility with [TransactionBuilder].
     * Please set the serialization scheme id and properties through the [ZKTransactionBuilder] constructor.
     */
    @Suppress("UNUSED_PARAMETER")
    fun toWireTransaction(services: ServicesForResolution, schemeId: Int): WireTransaction =
        throw UnsupportedOperationException(
            "This function is purposely disabled and only present for API compatibility with [TransactionBuilder]. " +
                "Please set the serialization scheme id and properties through the [ZKTransactionBuilder] constructor."
        )

    /**
     * This function is purposely disabled and only present for API compatibility with [TransactionBuilder].
     * Please set the serialization scheme id and properties through the [ZKTransactionBuilder] constructor.
     */
    @Suppress("UNUSED_PARAMETER")
    fun toWireTransaction(services: ServicesForResolution, schemeId: Int, properties: Map<Any, Any>): WireTransaction =
        throw UnsupportedOperationException(
            "This function is purposely disabled and only present for API compatibility with [TransactionBuilder]. " +
                "Please set the serialization scheme id and properties through the [ZKTransactionBuilder] constructor."
        )

    fun withItems(vararg items: Any) = apply { builder.withItems(*items) }
    fun addReferenceState(referencedStateAndRef: ReferencedStateAndRef<*>) = apply {
        builder.addReferenceState(referencedStateAndRef)
    }

    fun addInputState(stateAndRef: StateAndRef<*>) = apply { builder.addInputState(stateAndRef) }
    fun addAttachment(attachmentId: AttachmentId) = apply { builder.addAttachment(attachmentId) }
    fun addOutputState(state: TransactionState<*>) = apply { builder.addOutputState(state) }
    fun addOutputState(
        state: ContractState,
        contract: ContractClassName = requireNotNullContractClassName(state),
        notary: Party,
        encumbrance: Int? = null,
        constraint: AttachmentConstraint = AutomaticPlaceholderConstraint
    ) = apply { builder.addOutputState(state, contract, notary, encumbrance, constraint) }

    fun addOutputState(
        state: ContractState,
        contract: ContractClassName = requireNotNullContractClassName(state),
        constraint: AttachmentConstraint = AutomaticPlaceholderConstraint
    ) = apply { builder.addOutputState(state, contract, constraint) }

    fun addOutputState(state: ContractState, constraint: AttachmentConstraint) =
        apply { builder.addOutputState(state, constraint) }

    fun addCommand(arg: Command<*>) = apply { builder.addCommand(arg) }
    fun addCommand(data: CommandData, vararg keys: PublicKey) = apply { builder.addCommand(data, *keys) }
    fun addCommand(data: CommandData, keys: List<PublicKey>) = apply { builder.addCommand(data, keys) }
    fun setTimeWindow(timeWindow: TimeWindow) = apply { builder.setTimeWindow(timeWindow) }
    fun setTimeWindow(time: Instant, timeTolerance: Duration) = apply { builder.setTimeWindow(time, timeTolerance) }
    fun setPrivacySalt(privacySalt: PrivacySalt) = apply { builder.setPrivacySalt(privacySalt) }

    fun inputStates() = builder.inputStates()
    fun referenceStates() = builder.referenceStates()
    fun attachments() = builder.attachments()
    fun outputStates() = builder.outputStates()
    fun commands() = builder.commands()

    // Copied from [TransactionBuilder] because private
    private fun requireNotNullContractClassName(state: ContractState) =
        requireNotNull(state.requiredContractClassName) {
            """
        Unable to infer Contract class name because state class ${state::class.java.name} is not annotated with
        @BelongsToContract, and does not have an enclosing class which implements Contract. Either annotate ${state::class.java.name}
        with @BelongsToContract, or supply an explicit contract parameter to addOutputState().
            """.trimIndent().replace('\n', ' ')
        }
    /**
     * END: copy of [TransactionBuilder] API that delegates unchanged
     */
}

/**
 * Here follow copies of the `ServiceHub.signInitialTransaction(...)` variants, for our [ZKTransactionBuilder]
 */
private fun ServiceHub.signInitialTransaction(
    builder: ZKTransactionBuilder,
    publicKey: PublicKey,
    signatureMetadata: SignatureMetadata
): SignedTransaction {
    return builder.toSignedTransaction(keyManagementService, publicKey, signatureMetadata, this)
}

fun ServiceHub.signInitialTransaction(builder: ZKTransactionBuilder, publicKey: PublicKey) =
    signInitialTransaction(
        builder,
        publicKey,
        SignatureMetadata(myInfo.platformVersion, Crypto.findSignatureScheme(publicKey).schemeNumberID)
    )

fun ServiceHub.signInitialTransaction(builder: ZKTransactionBuilder): SignedTransaction =
    signInitialTransaction(
        builder,
        this.myInfo.legalIdentitiesAndCerts.first().owningKey // same as private ServiceHub.legalIdentityKey
    )

fun ServiceHub.signInitialTransaction(
    builder: ZKTransactionBuilder,
    signingPubKeys: Iterable<PublicKey>
): SignedTransaction {
    val it = signingPubKeys.iterator()
    var stx = signInitialTransaction(builder, it.next())
    while (it.hasNext()) {
        stx = addSignature(stx, it.next())
    }
    return stx
}
