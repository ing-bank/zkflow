package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.zkp.UtxoInfo
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.lazyMapped
import net.corda.core.node.NetworkParameters
import net.corda.core.node.ServiceHub
import net.corda.core.node.ServicesForResolution
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction
import java.security.PublicKey
import java.util.function.Predicate

fun ServiceHub.collectSerializedUtxosAndNonces(
    stateRefs: List<StateRef>,
    receivedStateInfo: List<UtxoInfo> = emptyList()
): Pair<List<ByteArray>, List<SecureHash>> {
    return stateRefs.map { stateRef ->
        // First see if we received the stateInfo before querying:
        if (receivedStateInfo.any { it.stateRef == stateRef }) {
            val utxoInfo = receivedStateInfo.single { it.stateRef == stateRef }
            utxoInfo.serializedContents to utxoInfo.nonce
        } else {
            val prevStx = validatedTransactions.getTransaction(stateRef.txhash)
                ?: error("Plaintext tx not found for hash ${stateRef.txhash}")

            val serializedUtxo = prevStx.tx
                .componentGroups.single { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }
                .components[stateRef.index].copyBytes()

            val nonce = prevStx.buildFilteredTransaction(Predicate { true })
                .filteredComponentGroups.single { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }
                .nonces[stateRef.index]

            serializedUtxo to nonce
        }
    }.unzip()
}

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

fun WireTransaction.zkCommandData() = commands.single().value as ZKCommandData

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

val SignedZKVerifierTransaction.dependencies: Set<SecureHash>
    get() = tx.dependencies

val ZKVerifierTransaction.dependencies: Set<SecureHash>
    get() = (inputs.asSequence() + references.asSequence()).map { it.txhash }.toSet()
