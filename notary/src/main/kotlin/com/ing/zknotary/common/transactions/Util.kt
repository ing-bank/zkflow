package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.crypto.blake2s256
import com.ing.zknotary.common.crypto.pedersen
import com.ing.zknotary.common.zkp.Witness
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.lazyMapped
import net.corda.core.node.NetworkParameters
import net.corda.core.node.ServicesForResolution
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import java.lang.IllegalArgumentException
import java.security.PublicKey
import java.util.function.Predicate
import kotlin.math.max

@DeleteForDJVM
fun WireTransaction.prettyPrint(): String {
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
    buf.appendln(" - COMMAND:  ${commands.single()}")
    addComponentList(buf, "ATTACHMENT HASHES", attachments)

    if (networkParametersHash != null) {
        buf.appendln(" - PARAMETERS HASH:  $networkParametersHash")
    }
    return buf.toString()
}

fun WireTransaction.zkCommandData() = commands.single().value as ZKCommandData

fun WireTransaction.toWitness(
    inputs: List<StateAndRef<ContractState>>
): Witness {
    loggerFor<WireTransaction>().debug("Creating Witness from ProverTx")

    // We turn wtx into ftx to get access to nonces, they are internal in wtx but visible in ftx
    val ftx = buildFilteredTransaction(Predicate { true })

    // Collect the nonces for the outputs pointed to by the inputs and references.
    val inputNonces = ftx.filteredComponentGroups.find { it.groupIndex == ComponentGroupEnum.INPUTS_GROUP.ordinal }?.nonces ?: error("Inputs group is missing")
    val referenceNonces = ftx.filteredComponentGroups.find { it.groupIndex == ComponentGroupEnum.REFERENCES_GROUP.ordinal }?.nonces ?: error("Inputs group is missing")

    return Witness(this, inputs, inputNonces, referenceNonces)
}

fun WireTransaction.toZKVerifierTransaction(
    proof: ByteArray,
    componentGroupLeafDigestService: DigestService = DigestService.blake2s256,
    nodeDigestService: DigestService = DigestService.pedersen
): ZKVerifierTransaction {
    loggerFor<WireTransaction>().debug("Converting ProverTx tot VerifierTx")

    // We turn wtx into ftx to get access to nonces and hashes, they are internal in wtx but visible in ftx
    val ftx = this.buildFilteredTransaction(Predicate { true })

    // IMPORTANT: this should only include the nonces for the components that are visible in the ZKVerifierTransaction
    val componentNonces = ftx.filteredComponentGroups.filter {
        it.groupIndex in listOf(
            ComponentGroupEnum.INPUTS_GROUP.ordinal,
            ComponentGroupEnum.REFERENCES_GROUP.ordinal,
            ComponentGroupEnum.NOTARY_GROUP.ordinal,
            ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
            ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
            ComponentGroupEnum.SIGNERS_GROUP.ordinal
        )
    }.map { it.groupIndex to it.nonces }.toMap()


    // TODO
    // This construction of the circuit id is temporary and will be replaced in the subsequent work.
    // The proper id must identify circuit and its version.
    val circuitId = zkCommandData().circuitId()

    val outputHashes = availableComponentHashes(ComponentGroupEnum.OUTPUTS_GROUP.ordinal, this, ftx)

    return ZKVerifierTransaction(
        proof,
        this.inputs,
        this.references,
        circuitId,

        this.commands.single().signers,

        this.notary ?: throw IllegalArgumentException("Notary cannot be null"),
        this.timeWindow,
        this.networkParametersHash,

        componentGroupLeafDigestService,
        nodeDigestService,

        outputHashes,
        ftx.groupHashes,
        componentNonces
    )
}

fun availableComponentHashes(groupIndex: Int, wtx: WireTransaction, ftx: FilteredTransaction): List<SecureHash> {
    val nonces = ftx.filteredComponentGroups.find { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }!!.nonces
    return wtx.componentGroups[groupIndex].components.mapIndexed { internalIndex, internalIt -> wtx.digestService.componentHash(nonces[internalIndex], internalIt) }
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

val SignedZKVerifierTransaction.dependencies: Set<SecureHash>
    get() = tx.dependencies

val ZKVerifierTransaction.dependencies: Set<SecureHash>
    get() = (inputs.asSequence() + references.asSequence()).map { it.txhash }.toSet()
