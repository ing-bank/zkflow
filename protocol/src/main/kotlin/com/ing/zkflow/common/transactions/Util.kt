@file:Suppress("TooManyFunctions")

package com.ing.zkflow.common.transactions

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.transactions.verification.ZKTransactionVerifierService
import com.ing.zkflow.common.zkp.ZKTransactionService
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.WritableUtxoInfoStorage
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.node.services.getCordaServiceFromConfig
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.PartialMerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.algorithm
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.Party
import net.corda.core.internal.FetchAttachmentsFlow
import net.corda.core.internal.SerializedStateAndRef
import net.corda.core.internal.lazyMapped
import net.corda.core.node.NetworkParameters
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SerializedBytes
import net.corda.core.transactions.ContractUpgradeWireTransaction
import net.corda.core.transactions.CoreTransaction
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.NotaryChangeWireTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.transactions.WireTransaction.Companion.resolveStateRefBinaryComponent
import java.security.PublicKey
import kotlin.reflect.KClass

/**
 * Note that this is the Java name, which ensures it can be loaded with Class.forName.
 * This is especially important with nested classes, which are designated as 'ContainerClass$NestedClass'
 * when using `jave.name`. Otherwise it would be 'ContainerClass.NestedClas', which results in a ClassNotFoundException.
 */

val KClass<out ContractState>.qualifiedStateClassName: String
    get() = java.name ?: error("Contract state classes must be a named class")

val KClass<out AttachmentConstraint>.qualifiedConstraintClassName: String
    get() = java.name ?: error("Attachment constraint classes must be a named class")

/**
 * Fully qualified name of ZKCommand class
 */
typealias ZKCommandClassName = String
typealias Proof = ByteArray

fun ServiceHub.collectUtxoInfos(
    stateRefs: List<StateRef>
): List<UtxoInfo> {
    val collectFromTransactionStorage: (StateRef) -> UtxoInfo =
        {
            val prevStx = validatedTransactions.getTransaction(it.txhash)
                ?: throw TransactionResolutionException(it.txhash, "Plaintext tx not found for hash ${it.txhash}")

            val serializedUtxo = prevStx.tx
                .componentGroups.single { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }
                .components[it.index].copyBytes()

            val nonce = prevStx.buildFilteredTransaction { true }
                .filteredComponentGroups.single { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }
                .nonces[it.index]

            val stateClass = prevStx.tx.outputs[it.index].data::class

            UtxoInfo.build(it, serializedUtxo, nonce, stateClass)
        }

    val collectFromUtxoInfoStorage: (StateRef) -> UtxoInfo =
        {
            getCordaServiceFromConfig<WritableUtxoInfoStorage>(ServiceNames.ZK_UTXO_INFO_STORAGE).getUtxoInfo(it)
                ?: throw UtxoInfoResolutionException(it)
        }

    fun collectSerializedUtxosAndNonces(stateRef: StateRef): UtxoInfo {
        // First we try to get from plaintext transaction storage, if that fails, from UtxoInfo storage
        return try {
            collectFromTransactionStorage(stateRef)
        } catch (e: TransactionResolutionException) {
            collectFromUtxoInfoStorage(stateRef)
        }
    }

    return stateRefs.map { stateRef ->
        collectSerializedUtxosAndNonces(stateRef)
    }
}

class UtxoInfoResolutionException(it: StateRef, message: String = "UtxoInfo resolution failure for $it") :
    FlowException(message)

@DeleteForDJVM
fun WireTransaction.prettyPrint(): String {
    val buf = StringBuilder()
    buf.appendLine("Wire Transaction:")

    fun addComponentList(buf: StringBuilder, name: String, componentList: List<*>) {
        if (componentList.isNotEmpty()) buf.appendLine(" - $name:")
        for ((index, component) in componentList.withIndex()) {
            buf.appendLine("\t[$index]:\t$component")
        }
    }

    addComponentList(buf, "REFS", references)
    addComponentList(buf, "INPUTS", inputs)
    addComponentList(buf, "OUTPUTS", outputs)
    buf.appendLine(" - COMMAND:  ${commands.single()}")
    addComponentList(buf, "ATTACHMENT HASHES", attachments)

    if (networkParametersHash != null) {
        buf.appendLine(" - PARAMETERS HASH:  $networkParametersHash")
    }
    return buf.toString()
}

val ZKTransactionBuilder.hasZKCommandData get() = commands().any { it.value is ZKCommandData }
val TraversableTransaction.hasZKCommandData get() = commands.any { it.value is ZKCommandData }
val TraversableTransaction.hasPrivateComponents get() = hasZKCommandData
val LedgerTransaction.hasZKCommandData get() = commands.any { it.value is ZKCommandData }

val ZKTransactionBuilder.commandData get() = commands().map { it.value }.filterIsInstance<ZKCommandData>()
val TraversableTransaction.commandData get() = commands.map { it.value }.filterIsInstance<ZKCommandData>()
val LedgerTransaction.commandData get() = commands.map { it.value }.filterIsInstance<ZKCommandData>()

val ZKTransactionBuilder.commandMetadata get() = commandData.map { it.metadata }
val TraversableTransaction.commandMetadata get() = commandData.map { it.metadata }
val LedgerTransaction.commandMetadata get() = commandData.map { it.metadata }
private const val TX_CONTAINS_NO_COMMANDS_WITH_METADATA = "This transaction does not contain any commands with metadata"

fun zkTransactionMetadata(
    commandMetadata: List<ResolvedZKCommandMetadata>,
    classLoader: ClassLoader
): ResolvedZKTransactionMetadata {
    assert(commandMetadata.isNotEmpty())
    val loadedCommandMetadata = commandMetadata.map {
        try {
            Class.forName(it.commandKClass.java.name, false, classLoader).asSubclass(ZKCommandData::class.java)
                .newInstance().metadata
        } catch (e: Exception) {
            error("Class definition not found in contract attachment for command: ${it.commandKClass.qualifiedName}")
        }
    }
    return ResolvedZKTransactionMetadata(loadedCommandMetadata)
}

fun TraversableTransaction.zkTransactionMetadata(classLoader: ClassLoader = ClassLoader.getSystemClassLoader()): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) zkTransactionMetadata(this.commandMetadata, classLoader) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

fun TraversableTransaction.zkTransactionMetadataOrNull(classLoader: ClassLoader = ClassLoader.getSystemClassLoader()): ResolvedZKTransactionMetadata? =
    if (this.hasZKCommandData) zkTransactionMetadata(this.commandMetadata, classLoader) else null

fun LedgerTransaction.zkTransactionMetadata(classLoader: ClassLoader = ClassLoader.getSystemClassLoader()): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) zkTransactionMetadata(this.commandMetadata, classLoader) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

fun ZKTransactionBuilder.zkTransactionMetadata(classLoader: ClassLoader = ClassLoader.getSystemClassLoader()): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) zkTransactionMetadata(this.commandMetadata, classLoader) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

fun FilteredTransaction.allComponentNonces(): Map<Int, List<SecureHash>> {
    return filteredComponentGroups.associate { it.groupIndex to it.nonces }
}

/**
 * This method correctly works only for full binary tree (for PartialMerkleTree it is not always the case),
 *
 */

fun PartialMerkleTree.getComponentHash(componentIndex: Int): SecureHash {

    fun getHeight(tree: PartialMerkleTree.PartialTree, level: Int = 0): Int {
        return when (tree) {
            is PartialMerkleTree.PartialTree.IncludedLeaf -> level
            is PartialMerkleTree.PartialTree.Node -> getHeight(tree.left, level + 1)
            is PartialMerkleTree.PartialTree.Leaf -> error("getHeight only works correctly with full trees")
        }
    }

    val height = getHeight(root)
    var mask = 1 shl (height - 1)
    var node = root
    if (height != 0) {
        while (mask != 0) {
            node = if (mask and componentIndex == 0)
                (node as PartialMerkleTree.PartialTree.Node).left
            else
                (node as PartialMerkleTree.PartialTree.Node).right
            mask = mask shr 1
        }
    }
    require(node is PartialMerkleTree.PartialTree.IncludedLeaf) { "Tree branch is longer than expected" }
    return node.hash
}

@Throws(AttachmentResolutionException::class, TransactionResolutionException::class)
@DeleteForDJVM
/**
 * Attention! Resulting LTX will differ from LTX that would be created from base WireTransaction, because we don't have access
 * to private UTXOs and components. Specifically, this means that component indexes of the outputs/utxos will be different.
 */
fun TraversableTransaction.zkToFilteredLedgerTransaction(services: ServiceHub): LedgerTransaction {
    val resolveIdentity: (PublicKey) -> Party? = { services.identityService.partyFromKey(it) }
    val resolveAttachment: (SecureHash) -> Attachment? = { services.attachments.openAttachment(it) }
    val resolveParameters: (SecureHash?) -> NetworkParameters? = {
        val hashToResolve = it ?: services.networkParametersService.defaultHash
        services.networkParametersService.lookup(hashToResolve)
    }

    val resolvedInputs = inputs.mapNotNull { ref -> resolveStateRefOrNull(ref, services) }.lazyMapped { star, _ -> star.toStateAndRef() }
    val resolvedReferences = references.mapNotNull { ref -> resolveStateRefOrNull(ref, services) }.lazyMapped { star, _ -> star.toStateAndRef() }

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
        PrivacySalt.createFor(id.algorithm), // We don't want to use real PrivacySalt so we create a fake one here
        resolvedNetworkParameters,
        resolvedReferences,
        DigestService(id.algorithm)
    )

    // Normally here transaction size is checked but in ZKP flow we don't really care because all our txs are fixed-size
    // checkTransactionSize(ltx, resolvedNetworkParameters.maxTransactionSize, serializedResolvedInputs, serializedResolvedReferences)

    return ltx
}

/**
 * First look into ZKVTX storage, if it doesn't help - look up in "normal" tx storage,
 * if no luck as well - we should only check utxo storage if we want to include private states as well,
 * in most cases in protocol we should avid that so be carefu
 */
fun resolveStateRef(stateRef: StateRef, services: ServiceHub, includePrivate: Boolean = false): SerializedBytes<TransactionState<ContractState>> {

    // Check ZKVTX storage
    try {
        val stx = services.getCordaServiceFromConfig<ZKWritableVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE).getTransaction(stateRef.txhash) ?: throw TransactionResolutionException(stateRef.txhash)

        if (!includePrivate) {
            if (stx.tx.isPrivateComponent(ComponentGroupEnum.OUTPUTS_GROUP, stateRef.index)) throw PrivateUtxoAccess()

            @Suppress("UNCHECKED_CAST")
            return stx.tx.componentGroups
                .firstOrNull { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }
                ?.components
                ?.get(stateRef.index) as SerializedBytes<TransactionState<ContractState>>?
                ?: throw UtxoNotFoundInsideTx(stateRef)
        }
    } catch (e: TransactionResolutionException) { /* This is fine */ }

    // Check "normal" tx storage
    try { return resolveStateRefBinaryComponent(stateRef, services) ?: throw UtxoNotFoundInsideTx(stateRef) } catch (e: TransactionResolutionException) { /* This is fine too */ }

    if (includePrivate) {
        // Only check utxo storage if we want to fetch private data
        return services.getCordaServiceFromConfig<WritableUtxoInfoStorage>(ServiceNames.ZK_UTXO_INFO_STORAGE)
            .getUtxoInfo(stateRef)?.let { utxoInfo ->
                SerializedBytes(utxoInfo.serializedContents)
            } ?: throw UtxoNotFound(stateRef)
    } else throw PublicUtxoNotFound(stateRef)
}

/**
 * Here we don't fail in case of private utxos - we just return null
 */
fun resolveStateRefOrNull(stateRef: StateRef, services: ServiceHub): SerializedStateAndRef? {
    val state = try {
        resolveStateRef(stateRef, services)
    } catch (ex: PrivateUtxoAccess) {
        null
    }
    return if (state != null) SerializedStateAndRef(state, stateRef) else null
}

class PublicUtxoNotFound(stateRef: StateRef) : Exception("Output not found for state ref $stateRef")
class UtxoNotFoundInsideTx(stateRef: StateRef) : Exception("Output not found for state ref $stateRef")
class UtxoNotFound(stateRef: StateRef) : Exception("Output not found for state ref $stateRef")
class PrivateUtxoAccess : Exception()

/**
 * Fetches the set of attachments required to verify the given transaction. If these are not already present, they will be fetched from
 * a remote peer.
 *
 * @param transaction The transaction to fetch attachments for
 * @return True if any attachments were fetched from a remote peer, false otherwise
 */
// TODO: This could be done in parallel with other fetches for extra speed.
@Suspendable
fun FlowLogic<*>.fetchMissingAttachments(tx: CoreTransaction, otherSide: FlowSession): Boolean {
    val attachmentIds = when (tx) {
        is WireTransaction -> tx.attachments.toSet()
        is ContractUpgradeWireTransaction -> setOf(tx.legacyContractAttachmentId, tx.upgradedContractAttachmentId)
        else -> return false
    }
    val downloads = subFlow(FetchAttachmentsFlow(attachmentIds, otherSide)).downloaded
    return (downloads.isNotEmpty())
}

val SignedZKVerifierTransaction.dependencies: Set<SecureHash>
    get() = tx.dependencies

val ZKVerifierTransaction.dependencies: Set<SecureHash>
    get() = (inputs.asSequence() + references.asSequence()).map { it.txhash }.toSet()

fun SignedTransaction.zkVerify(
    services: ServiceHub,
    checkSufficientSignatures: Boolean = true,
) {
    zkResolveAndCheckNetworkParameters(services)
    when (coreTransaction) {
        is NotaryChangeWireTransaction -> TODO("Not supported for now") // Perhaps just proxy to `verify(services, checkSufficientSignatures)`?
        is ContractUpgradeWireTransaction -> TODO("Not supported for now") // Perhaps just proxy to `verify(services, checkSufficientSignatures)`?
        else -> zkVerifyRegularTransaction(services, checkSufficientSignatures)
    }
}

private fun SignedTransaction.zkVerifyRegularTransaction(
    services: ServiceHub,
    checkSufficientSignatures: Boolean,
) {
    val zkTransactionVerifierService = ZKTransactionVerifierService(
        services,
        services.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)
    )

    zkTransactionVerifierService.verify(this, checkSufficientSignatures)
}

private fun SignedTransaction.zkResolveAndCheckNetworkParameters(services: ServiceHub) {
    val zkTxStorage: ZKVerifierTransactionStorage =
        services.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)

    val hashOrDefault = networkParametersHash ?: services.networkParametersService.defaultHash
    val txNetworkParameters = services.networkParametersService.lookup(hashOrDefault)
        ?: throw TransactionResolutionException(id)
    val groupedInputsAndRefs = (inputs + references).groupBy { it.txhash }
    groupedInputsAndRefs.map { entry ->
        val tx = zkTxStorage.getTransaction(entry.key)
            ?: throw TransactionResolutionException(id)
        val paramHash = tx.tx.networkParametersHash ?: services.networkParametersService.defaultHash
        val params = services.networkParametersService.lookup(paramHash) ?: throw TransactionResolutionException(id)
        if (txNetworkParameters.epoch < params.epoch)
            throw TransactionVerificationException.TransactionNetworkParameterOrderingException(
                id,
                entry.value.first(),
                txNetworkParameters,
                params
            )
    }
}

fun SignedTransaction.prove(
    services: ServiceHub
): SignedZKVerifierTransaction {
    val zkService: ZKTransactionService = services.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)
    return SignedZKVerifierTransaction(zkService.prove(tx), sigs)
}
