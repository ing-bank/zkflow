package com.ing.zkflow.common.transactions

import co.paralleluniverse.strands.Strand
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.network.ZKNetworkParametersServiceLoader
import com.ing.zkflow.common.serialization.ZKCustomSerializationScheme.Companion.CONTEXT_KEY_TRANSACTION_METADATA
import com.ing.zkflow.common.serialization.ZKCustomSerializationScheme.Companion.CONTEXT_KEY_ZK_NETWORK_PARAMETERS
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ComponentGroupEnum
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
import net.corda.core.internal.VisibleForTesting
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
class ZKTransactionBuilder private constructor(
    val builder: TransactionBuilder,
    val zkNetworkParameters: ZKNetworkParameters = ZKNetworkParametersServiceLoader.latest,
    // TransactionBuilder does not expose `inputsWithTransactionState` and `referencesWithTransactionState`, which are required for the ordered TransactionBuilder
    // to sort the states by name.
    val inputsWithTransactionState: ArrayList<StateAndRef<ContractState>> = arrayListOf(),
    val referencesWithTransactionState: ArrayList<TransactionState<ContractState>> = arrayListOf(),
    val window: TimeWindow? = null,
    val privacySalt: PrivacySalt = PrivacySalt(),
    val serviceHub: ServiceHub? = (Strand.currentStrand() as? FlowStateMachine<*>)?.serviceHub
) {
    constructor(notary: Party) : this(TransactionBuilder(notary))
    constructor(notary: Party, zkNetworkParameters: ZKNetworkParameters) : this(
        TransactionBuilder(notary),
        zkNetworkParameters = zkNetworkParameters
    )

    /**
     * Duplicated so that `toWireTransaction()` always uses the serialization settings
     */
    fun toWireTransaction(services: ServicesForResolution): WireTransaction {
        val resolvedTransactionMetadata = this.verify()

        val serializationProperties = mutableMapOf<Any, Any>(
            CONTEXT_KEY_ZK_NETWORK_PARAMETERS to zkNetworkParameters
        ).also {
            if (resolvedTransactionMetadata != null) {
                it[CONTEXT_KEY_TRANSACTION_METADATA] = resolvedTransactionMetadata
            }
        }

        return builder.toWireTransaction(services, zkNetworkParameters.serializationSchemeId, serializationProperties)
    }

    /**
     * This function should only ever be used for testing. It is required to test without custom serialization.
     */
    @VisibleForTesting
    internal fun toWireTransactionWithDefaultCordaSerializationForTesting(services: ServicesForResolution): WireTransaction {
        verify()
        return builder.toWireTransaction(services)
    }

    private fun verify(): ResolvedZKTransactionMetadata? {
        return if (this.hasZKCommandData) {
            val resolvedTransactionMetadata = this.zkTransactionMetadata()
            resolvedTransactionMetadata.verify(this)
            resolvedTransactionMetadata
        } else {
            null
        }
    }

    fun enforcePrivateInputsAndReferences(zkVerifierTransactionStorage: ZKVerifierTransactionStorage) {
        if (this.hasZKCommandData) {
            val resolvedTransactionMetadata = this.zkTransactionMetadata()
            val privateInputIndexes = resolvedTransactionMetadata.inputs.filter { it.mustBePrivate() }.map { it.index }
            val privateReferenceIndexes = (resolvedTransactionMetadata.references).filter { it.mustBePrivate() }.map { it.index }

            enforcePrivateUtxoForStateRefs(zkVerifierTransactionStorage, inputStates().filter { it.index in privateInputIndexes })
            enforcePrivateUtxoForStateRefs(zkVerifierTransactionStorage, referenceStates().filter { it.index in privateReferenceIndexes })
        }
    }

    private fun enforcePrivateUtxoForStateRefs(zkVerifierTransactionStorage: ZKVerifierTransactionStorage, stateRefs: List<StateRef>) {
        stateRefs.forEach { stateRef ->
            val tx = zkVerifierTransactionStorage.getTransaction(stateRef.txhash)?.tx
                ?: error("Could not enforce private UTXO for StateRef '$stateRef': ZKVerifierTransaction not found with ID: ${stateRef.txhash}")

            check(tx.isPrivateComponent(ComponentGroupEnum.OUTPUTS_GROUP, stateRef.index)) {
                "UTXO for StateRef '$stateRef' should be private, but it is public"
            }
        }
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
     * Duplicated so that it uses our custom `toWireTransaction()` function
     */
    fun toLedgerTransaction(services: ServiceHub) = toWireTransaction(services).toLedgerTransaction(services)

    fun verify(services: ServiceHub) = toLedgerTransaction(services).verify()

    /*
   * START: copy of [TransactionBuilder] API with changed behaviour
   */
    fun copy(): ZKTransactionBuilder = ZKTransactionBuilder(
        builder.copy(),
        zkNetworkParameters,
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

    @Suppress("ComplexMethod") // Copy of Corda TransactionBuilder
    fun withItems(vararg items: Any) = apply {
        items.forEach {
            when (it) {
                is StateAndRef<*> -> {
                    inputsWithTransactionState.add(it)
                }
                is ReferencedStateAndRef<*> -> {
                    referencesWithTransactionState.add(it.stateAndRef.state)
                }
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
        builder.addOutputState(state)
    }

    fun addOutputState(
        state: ContractState,
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
        state: ContractState,
        contract: ContractClassName = requireNotNullContractClassName(state),
        constraint: AttachmentConstraint = AutomaticPlaceholderConstraint
    ) = apply {
        builder.addOutputState(state, contract, constraint)
    }

    fun addOutputState(state: ContractState, constraint: AttachmentConstraint) =
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
