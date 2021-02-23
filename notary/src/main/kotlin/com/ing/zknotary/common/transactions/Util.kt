package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.toZKCommand
import com.ing.zknotary.common.crypto.blake2s256
import com.ing.zknotary.common.crypto.pedersen
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.lazyMapped
import net.corda.core.node.NetworkParameters
import net.corda.core.node.ServiceHub
import net.corda.core.node.ServicesForResolution
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import java.security.PublicKey
import kotlin.math.max

@DeleteForDJVM
fun ZKProverTransaction.prettyPrint(): String {
    val buf = StringBuilder()
    buf.appendln("Prover Transaction:")

    fun addComponentList(buf: StringBuilder, name: String, componentList: List<*>) {
        if (componentList.isNotEmpty()) buf.appendln(" - $name:")
        for ((index, component) in componentList.withIndex()) {
            buf.appendln("\t[$index]:\t$component")
        }
    }

    addComponentList(buf, "REFS", references)
    addComponentList(buf, "INPUTS", inputs)
    addComponentList(buf, "OUTPUTS", outputs)
    buf.appendln(" - COMMAND:  $command")
    addComponentList(buf, "ATTACHMENT HASHES", attachments)

    if (networkParametersHash != null) {
        buf.appendln(" - PARAMETERS HASH:  $networkParametersHash")
    }
    return buf.toString()
}

fun ZKProverTransaction.toZKVerifierTransaction(proof: ByteArray): ZKVerifierTransaction {
    loggerFor<ZKProverTransaction>().debug("Converting ProverTx tot VerifierTx")

    // IMPORTANT: this should only include the nonces for the components that are visible in the ZKVerifierTransaction
    val componentNonces = this.merkleTree.componentNonces.filterKeys {
        it in listOf(
            ComponentGroupEnum.INPUTS_GROUP.ordinal,
            ComponentGroupEnum.REFERENCES_GROUP.ordinal,
            ComponentGroupEnum.NOTARY_GROUP.ordinal,
            ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
            ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
            ComponentGroupEnum.SIGNERS_GROUP.ordinal
        )
    }

    val stateRefInputsFiller = ComponentPaddingConfiguration.Filler.StateRef(
        (this.padded.paddingConfiguration.filler(ComponentGroupEnum.INPUTS_GROUP) as ComponentPaddingConfiguration.Filler.StateAndRef).content.ref
    )
    val stateRefReferencesFiller = ComponentPaddingConfiguration.Filler.StateRef(
        (this.padded.paddingConfiguration.filler(ComponentGroupEnum.REFERENCES_GROUP) as ComponentPaddingConfiguration.Filler.StateAndRef).content.ref
    )
    val componentPadding = ComponentPaddingConfiguration.Builder()
        .inputs(
            this.padded.sizeOf(ComponentGroupEnum.INPUTS_GROUP),
            stateRefInputsFiller
        )
        .outputs(
            this.padded.sizeOf(ComponentGroupEnum.OUTPUTS_GROUP),
            this.padded.paddingConfiguration.filler(ComponentGroupEnum.OUTPUTS_GROUP)!!
        )
        .references(
            this.padded.sizeOf(ComponentGroupEnum.REFERENCES_GROUP),
            stateRefReferencesFiller
        )
        .attachments(
            this.padded.sizeOf(ComponentGroupEnum.ATTACHMENTS_GROUP),
            this.padded.paddingConfiguration.filler(ComponentGroupEnum.ATTACHMENTS_GROUP)!!
        )
        .signers(
            this.padded.sizeOf(ComponentGroupEnum.SIGNERS_GROUP),
            this.padded.paddingConfiguration.filler(ComponentGroupEnum.SIGNERS_GROUP)!!
        )
        .build()

    // TODO
    // This construction of the circuit id is temporary and will be replaced in the subsequent work.
    // The proper id must identify circuit and its version.
    val circuitId = command.value.circuitId()

    return ZKVerifierTransaction(
        proof,
        this.inputs.map { it.ref },
        this.references.map { it.ref },
        circuitId,

        this.command.signers,

        this.notary,
        this.timeWindow,
        this.networkParametersHash,

        this.componentGroupLeafDigestService,
        this.nodeDigestService,

        this.merkleTree.componentHashes[ComponentGroupEnum.OUTPUTS_GROUP.ordinal] ?: emptyList(),
        this.merkleTree.groupHashes,
        componentNonces,

        componentPadding
    )
}

/**
 * This function deterministically creates a [ZKProverTransaction] from a [WireTransaction].
 *
 * This is deterministic, because the [ZKProverTransaction] reuses the [PrivacySalt] from the WireTransaction.
 */
fun WireTransaction.toZKProverTransaction(
    services: ServiceHub,
    zkVerifierTransactionStorage: ZKVerifierTransactionStorage = services.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE),
    componentGroupLeafDigestService: DigestService = DigestService.blake2s256,
    nodeDigestService: DigestService = DigestService.pedersen
): ZKProverTransaction {
    loggerFor<WireTransaction>().debug("Converting WireTx to ProverTx")

    require(commands.size == 1) { "There must be exactly one command on a ZKProverTransaction" }

    // Look up the ZKid for each WireTransaction.id
    fun List<StateAndRef<*>>.mapToZkid(): List<StateAndRef<*>> {
        return map {
            val zkid = checkNotNull(zkVerifierTransactionStorage.map.get(it.ref.txhash)) {
                "Unexpectedly could not find the tx id map for ${it.ref.txhash}. Did you run ResolveTransactionsFlow before?"
            }
            StateAndRef(it.state, StateRef(zkid, it.ref.index))
        }
    }

    val ltx = toLedgerTransaction(services)

    return ZKProverTransaction(
        inputs = ltx.inputs.mapToZkid(),
        outputs = ltx.outputs.map { TransactionState(data = it.data, notary = it.notary) },
        references = ltx.references.mapToZkid(),
        command = ltx.commands.single().toZKCommand(),
        notary = ltx.notary!!,
        timeWindow = ltx.timeWindow,
        privacySalt = ltx.privacySalt,
        networkParametersHash = ltx.networkParameters?.serialize()?.hash,
        attachments = ltx.attachments.map { it.id },
        componentGroupLeafDigestService = componentGroupLeafDigestService,
        nodeDigestService = nodeDigestService
    )
}

@Suppress("LongParameterList")
fun WireTransaction.toZKProverTransaction(
    zkInputs: List<StateAndRef<*>>,
    zkReferences: List<StateAndRef<*>>,
    componentGroupLeafDigestService: DigestService = DigestService.blake2s256,
    nodeDigestService: DigestService = DigestService.pedersen
): ZKProverTransaction {
    loggerFor<WireTransaction>().debug("Converting WireTx to ProverTx")

    require(commands.size == 1) { "There must be exactly one command on a ZKProverTransaction" }

    return ZKProverTransaction(
        inputs = zkInputs,
        outputs = outputs.map { TransactionState(data = it.data, notary = it.notary) },
        references = zkReferences,
        command = commands.single().toZKCommand(),
        notary = notary!!,
        timeWindow = timeWindow,
        privacySalt = privacySalt,
        networkParametersHash = networkParametersHash,
        attachments = attachments,
        componentGroupLeafDigestService = componentGroupLeafDigestService,
        nodeDigestService = nodeDigestService
    )
}

@Throws(AttachmentResolutionException::class, TransactionResolutionException::class)
@DeleteForDJVM
fun WireTransaction.toLedgerTransaction(
    resolvedInputs: List<StateAndRef<ContractState>>,
    resolvedReferences: List<StateAndRef<ContractState>>,
    services: ServicesForResolution
): LedgerTransaction {

    val resolveIdentity: (PublicKey) -> Party? = { services.identityService.partyFromKey(it) }
    val resolveAttachment: (SecureHash) -> Attachment? = { services.attachments.openAttachment(it) }
    val resolveParameters: (SecureHash?) -> NetworkParameters? = {
        val hashToResolve = it ?: services.networkParametersService.defaultHash
        services.networkParametersService.lookup(hashToResolve)
    }

    // Look up public keys to authenticated identities.
    val authenticatedCommands = commands.lazyMapped { cmd, _ ->
        val parties = cmd.signers.mapNotNull { pk -> resolveIdentity(pk) }
        CommandWithParties(cmd.signers, parties, cmd.value)
    }

    val resolvedAttachments =
        attachments.lazyMapped { att, _ -> resolveAttachment(att) ?: throw AttachmentResolutionException(att) }

    val resolvedNetworkParameters =
        resolveParameters(networkParametersHash) ?: throw TransactionResolutionException.UnknownParametersException(
            id,
            networkParametersHash!!
        )

    val ltx = LedgerTransaction.createForSandbox(
        resolvedInputs,
        outputs,
        authenticatedCommands,
        resolvedAttachments,
        id,
        notary,
        timeWindow,
        privacySalt,
        resolvedNetworkParameters,
        resolvedReferences,
        DigestService.sha2_256
    )

    // Normally here transaction size is checked but in ZKP flow we don't really care because all our txs are fixed-size
    // checkTransactionSize(ltx, resolvedNetworkParameters.maxTransactionSize, serializedResolvedInputs, serializedResolvedReferences)

    return ltx
}

/**
 * Extends a list with a default value.
 */
fun <T> List<T>.pad(n: Int, default: T) = List(max(size, n)) {
    if (it < size)
        this[it]
    else {
        default
    }
}

fun <T> List<T>.wrappedPad(n: Int, default: T) =
    map { PaddingWrapper.Original(it) }.pad(n, PaddingWrapper.Filler(default))

fun <T> T?.wrappedPad(default: T) =
    if (this == null) PaddingWrapper.Filler(default) else PaddingWrapper.Original(this)

val SignedZKVerifierTransaction.dependencies: Set<SecureHash>
    get() = tx.dependencies

val ZKVerifierTransaction.dependencies: Set<SecureHash>
    get() = (inputs.asSequence() + references.asSequence()).map { it.txhash }.toSet()
