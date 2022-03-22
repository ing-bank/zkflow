@file:Suppress("TooManyFunctions")

package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.ZKTransactionService
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.node.services.ServiceNames
import com.ing.zkflow.node.services.WritableUtxoInfoStorage
import com.ing.zkflow.node.services.ZKVerifierTransactionStorage
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
import net.corda.core.identity.Party
import net.corda.core.internal.SerializedStateAndRef
import net.corda.core.internal.lazyMapped
import net.corda.core.node.NetworkParameters
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.SerializedBytes
import net.corda.core.transactions.ContractUpgradeWireTransaction
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

fun TraversableTransaction.zkTransactionMetadata(): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) ResolvedZKTransactionMetadata(this.commandMetadata) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

fun TraversableTransaction.zkTransactionMetadataOrNull(): ResolvedZKTransactionMetadata? =
    if (this.hasZKCommandData) ResolvedZKTransactionMetadata(this.commandMetadata) else null

fun LedgerTransaction.zkTransactionMetadata(): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) ResolvedZKTransactionMetadata(this.commandMetadata) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

fun ZKTransactionBuilder.zkTransactionMetadata(): ResolvedZKTransactionMetadata =
    if (this.hasZKCommandData) ResolvedZKTransactionMetadata(this.commandMetadata) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)

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
fun TraversableTransaction.zkToLedgerTransaction(
    services: ServiceHub
): LedgerTransaction {
    val resolveIdentity: (PublicKey) -> Party? = { services.identityService.partyFromKey(it) }
    val resolveAttachment: (SecureHash) -> Attachment? = { services.attachments.openAttachment(it) }
    val resolveParameters: (SecureHash?) -> NetworkParameters? = {
        val hashToResolve = it ?: services.networkParametersService.defaultHash
        services.networkParametersService.lookup(hashToResolve)
    }
    val resolveStateRefAsSerializedFromTransactionStorage: (StateRef) -> SerializedBytes<TransactionState<ContractState>>? =
        {
            resolveStateRefBinaryComponent(
                it,
                services
            )
        }

    val resolveStateRefAsSerializedFromUtxoInfoStorage: (StateRef) -> SerializedBytes<TransactionState<ContractState>>? =
        {
            services.getCordaServiceFromConfig<WritableUtxoInfoStorage>(ServiceNames.ZK_UTXO_INFO_STORAGE)
                .getUtxoInfo(it)?.let { utxoInfo ->
                    SerializedBytes(utxoInfo.serializedContents)
                }
        }

    fun resolveStateRefAsSerialized(stateRef: StateRef): SerializedBytes<TransactionState<ContractState>>? {
        // First we try to get from plaintext transaction storage, if that fails, from UtxoInfo storage
        return try {
            resolveStateRefAsSerializedFromTransactionStorage(stateRef)
        } catch (e: TransactionResolutionException) {
            resolveStateRefAsSerializedFromUtxoInfoStorage(stateRef)
        }
    }

    val serializedResolvedInputs = inputs.map { ref ->
        SerializedStateAndRef(resolveStateRefAsSerialized(ref) ?: throw TransactionResolutionException(ref.txhash), ref)
    }
    val resolvedInputs = serializedResolvedInputs.lazyMapped { star, _ -> star.toStateAndRef() }

    val serializedResolvedReferences = references.map { ref ->
        SerializedStateAndRef(resolveStateRefAsSerialized(ref) ?: throw TransactionResolutionException(ref.txhash), ref)
    }
    val resolvedReferences = serializedResolvedReferences.lazyMapped { star, _ -> star.toStateAndRef() }

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
    val ltx = zkToLedgerTransaction(services, checkSufficientSignatures)
    // This fails with a weird db access error, so we use ltx.verify
    // services.transactionVerifierService.verify(ltx).getOrThrow()

    // Check contract rules for public components
    ltx.verify()

    // Check contract rules for private components
    val zkService: ZKTransactionService = services.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)
    zkService.run(tx)
}

fun SignedTransaction.zkToLedgerTransaction(
    services: ServiceHub,
    checkSufficientSignatures: Boolean = true
): LedgerTransaction {
    if (checkSufficientSignatures) {
        verifyRequiredSignatures() // It internally invokes checkSignaturesAreValid().
    } else {
        checkSignaturesAreValid()
    }
    // We need parameters check here, because finality flow calls stx.toLedgerTransaction() and then verify.
    zkResolveAndCheckNetworkParameters(services)
    return tx.zkToLedgerTransaction(services)
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
