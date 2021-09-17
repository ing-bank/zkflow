package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.WritableUtxoInfoStorage
import com.ing.zknotary.node.services.ZKVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.DigestService
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
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.NotaryChangeWireTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.transactions.WireTransaction.Companion.resolveStateRefBinaryComponent
import java.security.PublicKey
import java.util.function.Predicate
import kotlin.reflect.KClass

/**
 * Note that this is the Java name, which ensures it can be loaded with Class.forName.
 * This is especially important with nested classes, which are designated as 'ContainerClass$NestedClass'
 * when using `jave.name`. Otherwise it would be 'ContainerClass.NestedClas', which results in a ClassNotFoundException.
 */

val KClass<out ContractState>.qualifiedStateClassName: String
    get() = java.name ?: error("Contract state classes must be a named class")

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

            val nonce = prevStx.buildFilteredTransaction(Predicate { true })
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

fun TraversableTransaction.zkTransactionMetadata() =
    (commands.first().value as ZKTransactionMetadataCommandData).transactionMetadata.resolved

@Throws(AttachmentResolutionException::class, TransactionResolutionException::class)
@DeleteForDJVM
// TODO: This function will have to be the one that is called from anywhere we need a LedgerTransaction.
// Currently in those locations, the 'standard' wtx.toLedgerTransaction is still called.
fun WireTransaction.zkToLedgerTransaction(
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
        privacySalt,
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
    ltx.verify()
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
