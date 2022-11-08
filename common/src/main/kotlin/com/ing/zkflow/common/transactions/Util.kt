@file:Suppress("TooManyFunctions")

package com.ing.zkflow.common.transactions

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.UtxoInfoStorage
import com.ing.zkflow.common.node.services.WritableUtxoInfoStorage
import com.ing.zkflow.common.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.util.appendLine
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
import net.corda.core.internal.isUploaderTrusted
import net.corda.core.internal.lazyMapped
import net.corda.core.node.NetworkParameters
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.internal.AttachmentsClassLoader
import net.corda.core.transactions.ContractUpgradeWireTransaction
import net.corda.core.transactions.CoreTransaction
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.LedgerTransaction
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

val ZKTransactionBuilder.zkCommandData get() = commands().map { it.value }.filterIsInstance<ZKCommandData>()
val TraversableTransaction.zkCommandData get() = commands.map { it.value }.filterIsInstance<ZKCommandData>()
val LedgerTransaction.zkCommandData get() = commands.map { it.value }.filterIsInstance<ZKCommandData>()

val ZKTransactionBuilder.commandMetadata get() = zkCommandData.map { it.metadata }
val TraversableTransaction.commandMetadata get() = zkCommandData.map { it.metadata }
val LedgerTransaction.commandMetadata get() = zkCommandData.map { it.metadata }

private const val TX_CONTAINS_NO_COMMANDS_WITH_METADATA = "This transaction does not contain any commands with metadata"
private const val NETWORKS_PARAMS_MISSING = "NetworkParameters not found"

/**
 * Classloader is required, (so not system classloader unless intended), because
 * the metadata needs to be loaded for the contract as defined in the contract attachment
 * for the transaction we want the metadata for.
 */
fun zkTransactionMetadata(
    commandMetadata: List<ResolvedZKCommandMetadata>,
    classLoader: ClassLoader
): ResolvedZKTransactionMetadata {
    require(commandMetadata.isNotEmpty()) { TX_CONTAINS_NO_COMMANDS_WITH_METADATA }
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

fun getClassLoaderFromContractAttachment(tx: TraversableTransaction, serviceHub: ServiceHub): ClassLoader {
    return getClassLoaderFromContractAttachment(tx.id, tx.attachments, tx.networkParametersHash, serviceHub)
}

fun getClassLoaderFromContractAttachment(
    txId: SecureHash,
    attachmentIds: List<SecureHash>,
    networkParametersHash: SecureHash?,
    serviceHub: ServiceHub
): ClassLoader {
    val hashToResolve = networkParametersHash ?: serviceHub.networkParametersService.defaultHash
    val params =
        serviceHub.networkParametersService.lookup(hashToResolve) ?: throw TransactionResolutionException.UnknownParametersException(
            SecureHash.zeroHash,
            hashToResolve
        )
    val attachments = attachmentIds.map { serviceHub.attachments.openAttachment(it) ?: error("Attachment ($it) not found") }

    return getClassLoaderFromContractAttachment(txId, attachments, params, serviceHub)
}

fun getClassLoaderFromContractAttachment(txId: SecureHash, attachments: List<Attachment>, params: NetworkParameters): ClassLoader {
    return AttachmentsClassLoader(attachments, params, txId, { it.isUploaderTrusted() }, ClassLoader.getSystemClassLoader())
}

fun getClassLoaderFromContractAttachment(
    txId: SecureHash,
    attachments: List<Attachment>,
    params: NetworkParameters,
    serviceHub: ServiceHub
): ClassLoader {
    return AttachmentsClassLoader(attachments, params, txId, { it.isUploaderTrusted() }, serviceHub.getAppContext().classLoader)
}
fun TraversableTransaction.zkTransactionMetadata(serviceHub: ServiceHub): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) zkTransactionMetadata(
        this.commandMetadata,
        getClassLoaderFromContractAttachment(this, serviceHub)
    ) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

fun TraversableTransaction.zkTransactionMetadataOrNull(serviceHub: ServiceHub): ResolvedZKTransactionMetadata? =
    if (this.hasZKCommandData) zkTransactionMetadata(this.commandMetadata, getClassLoaderFromContractAttachment(this, serviceHub)) else null

fun LedgerTransaction.zkTransactionMetadata(): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) zkTransactionMetadata(
        this.commandMetadata,
        getClassLoaderFromContractAttachment(id, attachments, networkParameters ?: error(NETWORKS_PARAMS_MISSING))
    ) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

fun LedgerTransaction.zkTransactionMetadataOrNull(): ResolvedZKTransactionMetadata? =
    if (this.hasZKCommandData) zkTransactionMetadata(
        this.commandMetadata,
        getClassLoaderFromContractAttachment(id, attachments, networkParameters ?: error(NETWORKS_PARAMS_MISSING))
    ) else null

fun ZKTransactionBuilder.zkTransactionMetadata(serviceHub: ServiceHub): ResolvedZKTransactionMetadata {
    return if (this.hasZKCommandData) {
        zkTransactionMetadata(
            this.commandMetadata,
            // Random SHA looks a bit weird here, but it is not really used inside ClassLoader creation, so it's not worth to build WireTx here just because of this id
            getClassLoaderFromContractAttachment(
                SecureHash.randomSHA256(),
                attachments(),
                serviceHub.networkParametersService.currentHash,
                serviceHub
            )
        )
    } else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)
}

fun TraversableTransaction.zkTransactionMetadata(classLoader: ClassLoader): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) zkTransactionMetadata(this.commandMetadata, classLoader) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

fun TraversableTransaction.zkTransactionMetadataOrNull(classLoader: ClassLoader): ResolvedZKTransactionMetadata? =
    if (this.hasZKCommandData) zkTransactionMetadata(this.commandMetadata, classLoader) else null

fun LedgerTransaction.zkTransactionMetadata(classLoader: ClassLoader): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) zkTransactionMetadata(this.commandMetadata, classLoader) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

fun ZKTransactionBuilder.zkTransactionMetadata(classLoader: ClassLoader = this.javaClass.classLoader): ResolvedZKTransactionMetadata =
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

/**
 * Turns a ZKVerifierTransaction into a filtered LedgerTransaction.
 * This LedgerTransaction will only contain the publicly visible components of the ZKVerifierTransaction.
 * This LedgerTransaction can be used to validate the public components of the transaction.
 *
 * Attention! Resulting LTX will differ from LTX that would be created from base WireTransaction, because we don't have access
 * to private UTXOs and components. Specifically, this means that component indexes of the outputs/utxos will be different.
 */
@Throws(AttachmentResolutionException::class, TransactionResolutionException::class)
@DeleteForDJVM
fun ZKVerifierTransaction.zkToFilteredLedgerTransaction(
    services: ServiceHub,
    vtxStorage: ZKVerifierTransactionStorage,
): LedgerTransaction {
    val resolveIdentity: (PublicKey) -> Party? = { services.identityService.partyFromKey(it) }
    val resolveAttachment: (SecureHash) -> Attachment? = { services.attachments.openAttachment(it) }
    val resolveParameters: (SecureHash?) -> NetworkParameters? = {
        val hashToResolve = it ?: services.networkParametersService.defaultHash
        services.networkParametersService.lookup(hashToResolve)
    }

    val resolvedInputs =
        inputs.mapNotNull { ref -> resolvePublicStateRefOrNull(ref, vtxStorage) }.lazyMapped { star, _ -> star.toStateAndRef() }
    val resolvedReferences =
        references.mapNotNull { ref -> resolvePublicStateRefOrNull(ref, vtxStorage) }.lazyMapped { star, _ -> star.toStateAndRef() }

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

    return LedgerTransaction.createForSandbox(
        resolvedInputs,
        outputs,
        authenticatedCommands,
        resolvedAttachments,
        id,
        notary,
        timeWindow,
        /**
         * We cannot use real PrivacySalt because we don't have it in a ZKVerifierTransaction,
         * but luckily we don't really need it in LTX to verify smart contract rules, so we create a dummy one
         */
        PrivacySalt.createFor(id.algorithm),
        resolvedNetworkParameters,
        resolvedReferences,
        DigestService(id.algorithm)
    )

    // Normally here transaction size is checked but in ZKP flow we don't really care because all our txs are fixed-size
    // checkTransactionSize(ltx, resolvedNetworkParameters.maxTransactionSize, serializedResolvedInputs, serializedResolvedReferences)
}

/**
 * Try to find a state in ZKVerifierTransactionStorage
 * This will only return public states. If a private component is accessed, an exception is thrown.
 */
fun resolvePublicStateRef(
    stateRef: StateRef,
    vtxStorage: ZKVerifierTransactionStorage,
): SerializedBytes<TransactionState<ContractState>> {
    val svtx = vtxStorage.getTransaction(stateRef.txhash) ?: throw TransactionResolutionException(stateRef.txhash)
    if (svtx.tx.isPrivateComponent(ComponentGroupEnum.OUTPUTS_GROUP, stateRef.index)) throw PrivateUtxoAccess(stateRef)

    @Suppress("UNCHECKED_CAST")
    return svtx.tx.componentGroups
        .firstOrNull { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }
        ?.components
        ?.get(stateRef.index) as? SerializedBytes<TransactionState<ContractState>>
        ?: throw UtxoNotFoundInsideTx(stateRef)
}

/**
 * Resolve a public or private state. Different from [resolvePublicStateRef], this will not fail on accessing private state data.
 * - First, we check the [ZKVerifierTransactionStorage] to see if we can find it in there as a public state. The state may be there if
 * we received it as a public part of a zkvtx that is part of a chain we are participant to, or if we are an observer.
 * - Next, we check the normal Corda transaction storage. This contains full public and private transaction data. The state may be found
 * there if we were a direct participant to a private transaction.
 * - Finally, if we do not have it in normal storage, we may have it in [UtxoInfoStorage]. This would be the case when we were not part
 * of the original transaction that created it, but our counterparty revealed it to us by sending it to us separately with its backchain.
 */
fun resolvePublicOrPrivateStateRef(
    stateRef: StateRef,
    serviceHub: ServiceHub,
    vtxStorage: ZKVerifierTransactionStorage,
    utxoInfoStorage: UtxoInfoStorage,
): SerializedBytes<TransactionState<ContractState>> {
    return try {
        resolvePublicStateRef(stateRef, vtxStorage)
    } catch (e: TransactionResolutionException) { /* Move on to searching normal transaction storage */
        try {
            return resolveStateRefBinaryComponent(stateRef, serviceHub) ?: throw UtxoNotFoundInsideTx(stateRef)
        } catch (e: TransactionResolutionException) { /* Move on to searching UtxoInfo storage */
            utxoInfoStorage.getUtxoInfo(stateRef)
                ?.let { utxoInfo -> SerializedBytes(utxoInfo.serializedContents) }
                ?: throw UtxoInfoNotFound(stateRef)
        }
    }
}

/**
 * Here we don't fail in case of private utxos - we just return null
 */
fun resolvePublicStateRefOrNull(stateRef: StateRef, vtxStorage: ZKVerifierTransactionStorage): SerializedStateAndRef? {
    val state = try {
        resolvePublicStateRef(stateRef, vtxStorage)
    } catch (ex: PrivateUtxoAccess) {
        null
    }
    return if (state != null) SerializedStateAndRef(state, stateRef) else null
}

class UtxoNotFoundInsideTx(stateRef: StateRef) :
    TransactionResolutionException(stateRef.txhash, "Output not found at index ${stateRef.index} in transaction with id ${stateRef.txhash}")

class UtxoInfoNotFound(stateRef: StateRef) : TransactionResolutionException(stateRef.txhash, "UtxoInfo not found for state ref $stateRef")
class PrivateUtxoAccess(stateRef: StateRef) :
    TransactionResolutionException(stateRef.txhash, "Not allowed to access private component at index ${stateRef.index} in public context")

/**
 * Fetches the set of attachments required to verify the given transaction. If these are not already present, they will be fetched from
 * a remote peer.
 *
 * @param transaction The transaction to fetch attachments for
 * @return True if any attachments were fetched from a remote peer, false otherwise
 */
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
