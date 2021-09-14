package com.ing.zknotary.common.transactions

import co.paralleluniverse.strands.Strand
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zknotary.common.serialization.bfl.BFLSerializationScheme
import com.ing.zknotary.common.transactions.StateOrdering.ordered
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndContract
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
    val builder: TransactionBuilder,
    private val serializationSchemeId: Int = BFLSerializationScheme.SCHEME_ID,
    private val serializationProperties: Map<Any, Any> = emptyMap(),
    // TransactionBuilder does not expose `inputsWithTransactionState` and `referencesWithTransactionState`, which are required for the ordered TransactionBuilder
    // to sort the states by name.
    val inputsWithTransactionState: ArrayList<StateAndRef<ContractState>> = arrayListOf<StateAndRef<ContractState>>(),
    val referencesWithTransactionState: ArrayList<TransactionState<ContractState>> = arrayListOf<TransactionState<ContractState>>(),
    val window: TimeWindow? = null,
    val privacySalt: PrivacySalt = PrivacySalt(),
    val serviceHub: ServiceHub? = (Strand.currentStrand() as? FlowStateMachine<*>)?.serviceHub
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

    init {
        outputStates().forEach { enforceZKContractStates(it.data) }
    }

    private fun enforceZKContractStates(state: ContractState) {
        require(state is ZKContractState) { "Can only use ZKContractStates as output" }
    }

    private companion object {
        // Copied from private `TransactionBuilder.defaultLockId`
        private fun defaultLockId() = (Strand.currentStrand() as? FlowStateMachine<*>)?.id?.uuid ?: UUID.randomUUID()
    }

    /**
     * Duplicated so that `toWireTransaction()` always uses the serialization settings
     */
    fun toWireTransaction(services: ServicesForResolution): WireTransaction {
        val command = commands().firstOrNull() ?: error("At least one command is required for a private transaction")
        val zkCommand = command.value as? ZKTransactionMetadataCommandData
            ?: error("This first command must implement ZKTransactionMetadataCommandData")
        val resolvedTransactionMetadata = zkCommand.transactionMetadata.resolved()

        resolvedTransactionMetadata.verify(this)
        //
        val serializationProperties = mapOf<Any, Any>(
            BFLSerializationScheme.CONTEXT_KEY_CIRCUIT to zkCommand.circuit,
            BFLSerializationScheme.CONTEXT_KEY_TRANSACTION_METADATA to resolvedTransactionMetadata
        )

        val orderedBuilder = ordered().builder
        return orderedBuilder.toWireTransaction(services, serializationSchemeId, serializationProperties)
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
     * The `inputsWithTransactionState and referencesWithTransactionState ore private members of TransactionBuilder,
     * so we have to recreate the TransactionBuilder by adding each state individually instead of passing them to the constructor (which does not set them).
     */
    private fun reconstructTransactionBuilder(
        notary: Party? = null,
        inputs: MutableList<StateAndRef<*>> = arrayListOf(),
        attachments: MutableList<AttachmentId> = arrayListOf(),
        outputs: MutableList<TransactionState<ContractState>> = arrayListOf(),
        commands: MutableList<Command<*>> = arrayListOf(),
        window: TimeWindow? = null,
        privacySalt: PrivacySalt = PrivacySalt(),
        references: MutableList<ReferencedStateAndRef<*>> = arrayListOf(),
        serviceHub: ServiceHub? = (Strand.currentStrand() as? FlowStateMachine<*>)?.serviceHub,
    ): TransactionBuilder {
        val transactionBuilder =
            TransactionBuilder(notary = notary, window = window, privacySalt = privacySalt, serviceHub = serviceHub)
        @Suppress("SpreadOperator")
        transactionBuilder.withItems(
            *inputs.toTypedArray(),
            *outputs.toTypedArray(),
            *references.toTypedArray(),
            *commands.toTypedArray(),
            *attachments.toTypedArray()
        )
        return transactionBuilder
    }

    /**
     * Reorders the states in the underlying TransactionBuilder so that these are lexicographically ordered by their class name.
     * In case two states have the same class name, the state that was added before the other in the ZKTransactionBuilder should come first.
     * Zinc does not support polymorphism, this implies that List where A is an interface is an invalid input for Zinc. To pass such a list to Zinc, we group the list of states w.r.t. their type;
     * the list will further be split into sublists containing the same type states.
     */
    fun ordered(): ZKTransactionBuilder {
        // Order outputs by classname.
        val orderedOutputs = builder.outputStates().ordered()

        // Order inputs and references by their state's classname.
        val orderedInputs = inputsWithTransactionState.ordered()
        // combine states and references
        val refs = referencesWithTransactionState.zip(builder.referenceStates()) { state, ref -> ReferencedStateAndRef(StateAndRef(state, ref)) }
        val orderedRefs = refs.ordered()

        val orderedBuilder = reconstructTransactionBuilder(
            notary = notary!!,
            outputs = orderedOutputs.toMutableList(),
            inputs = orderedInputs.toMutableList(),
            attachments = builder.attachments().toMutableList(),
            commands = builder.commands().toMutableList(),
            references = orderedRefs.toMutableList(),
            window = window,
            privacySalt = privacySalt,
            serviceHub = serviceHub
        )
        return ZKTransactionBuilder(orderedBuilder, serializationSchemeId, serializationProperties)
    }

    /**
     * Duplicated so that it uses our custom `toWireTransaction()` function
     */
    fun toLedgerTransaction(services: ServiceHub) = toWireTransaction(services).toLedgerTransaction(services)

    fun verify(services: ServiceHub) = toLedgerTransaction(services).verify()

    /*
   * START: copy of [TransactionBuilder] API with changed behaviour
   */
    fun copy(): ZKTransactionBuilder = ZKTransactionBuilder(
        builder.copy(),
        serializationSchemeId,
        serializationProperties,
        inputsWithTransactionState,
        referencesWithTransactionState,
        window,
        privacySalt
    )
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

    fun withItems(vararg items: Any) = apply {
        items.forEach {
            when (it) {
                is StateAndRef<*> -> { inputsWithTransactionState.add(it) }
                is ReferencedStateAndRef<*> -> { referencesWithTransactionState.add(it.stateAndRef.state) }
                is TransactionState<*> -> { enforceZKContractStates(it.data) }
                is StateAndContract -> { enforceZKContractStates(it.state) }
            }
        }
        builder.withItems(*items)
    }

    fun addReferenceState(referencedStateAndRef: ReferencedStateAndRef<*>) = apply {
        referencesWithTransactionState.add(referencedStateAndRef.stateAndRef.state)
        builder.addReferenceState(referencedStateAndRef)
    }

    fun addInputState(stateAndRef: StateAndRef<*>) = apply {
        inputsWithTransactionState.add(stateAndRef)
        builder.addInputState(stateAndRef)
    }

    fun addAttachment(attachmentId: AttachmentId) = apply { builder.addAttachment(attachmentId) }

    fun addOutputState(state: TransactionState<*>) = apply {
        enforceZKContractStates(state.data)
        builder.addOutputState(state)
    }

    fun addOutputState(
        state: ZKContractState,
        contract: ContractClassName = requireNotNullContractClassName(state),
        notary: Party,
        encumbrance: Int? = null,
        constraint: AttachmentConstraint = AutomaticPlaceholderConstraint
    ) = apply {
        builder.addOutputState(
            state,
            contract,
            notary,
            encumbrance,
            constraint
        )
    }

    fun addOutputState(
        state: ZKContractState,
        contract: ContractClassName = requireNotNullContractClassName(state),
        constraint: AttachmentConstraint = AutomaticPlaceholderConstraint
    ) = apply {
        builder.addOutputState(state, contract, constraint)
    }

    fun addOutputState(state: ZKContractState, constraint: AttachmentConstraint) =
        apply {
            builder.addOutputState(state, constraint)
        }

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
