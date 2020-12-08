package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.toZKCommand
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ZKProverTransactionStorage
import com.ing.zknotary.node.services.ZKWritableProverTransactionStorage
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.ServiceHub
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import java.nio.ByteBuffer
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
    val circuitId = SecureHash.sha256(ByteBuffer.allocate(4).putInt(this.command.value.id).array())

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
    zkProverTransactionStorage: ZKProverTransactionStorage,
    componentGroupLeafDigestService: DigestService,
    nodeDigestService: DigestService = componentGroupLeafDigestService
): ZKProverTransaction {
    loggerFor<WireTransaction>().debug("Converting WireTx to ProverTx")

    require(commands.size == 1) { "There must be exactly one command on a ZKProverTransaction" }

    // Look up the ZKid for each WireTransaction.id
    fun List<StateAndRef<*>>.mapToZkid(): List<StateAndRef<*>> {
        return map {
            val zkid = checkNotNull(zkProverTransactionStorage.map.get(it.ref.txhash)) {
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
fun SignedTransaction.toSignedZKVerifierTransaction(
    services: ServiceHub,
    zkSigs: List<TransactionSignature>,
    zkProverTransactionStorage: ZKWritableProverTransactionStorage,
    zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage,
    zkTransactionService: ZKTransactionService,
    persist: Boolean = true
): SignedZKVerifierTransaction {
    loggerFor<SignedTransaction>().debug("Converting SignedTx to SignedVerifierTx")

    val wtx = coreTransaction as WireTransaction
    val ptx = wtx.toZKProverTransaction(
        services,
        zkProverTransactionStorage,
        componentGroupLeafDigestService = BLAKE2s256DigestService,
        nodeDigestService = PedersenDigestService
    )
    val witness = ptx.toWitness(zkProverTransactionStorage)

    val proof = zkTransactionService.prove(witness)
    val vtx = witness.transaction.toZKVerifierTransaction(proof)

    val sptx = SignedZKProverTransaction(ptx, zkSigs)
    val svtx = SignedZKVerifierTransaction(vtx, zkSigs)

    if (persist) {
        zkProverTransactionStorage.map.put(this, ptx)
        zkVerifierTransactionStorage.map.put(this, vtx)
        zkProverTransactionStorage.addTransaction(sptx)
        zkVerifierTransactionStorage.addTransaction(svtx)
    }

    return svtx
}

fun ZKProverTransaction.toWitness(
    zkProverTransactionStorage: ZKProverTransactionStorage
): Witness {
    loggerFor<ZKProverTransaction>().debug("Creating Witness from ProverTx")
    // Because the PrivacySalt of the WireTransaction is reused to create the ProverTransactions,
    // the nonces are also identical from WireTransaction to ZKProverTransaction.
    // This means we can collect the UTXO nonces for the inputs and references of the wiretransaction and it should
    // just work.
    // When we move to full backchain privacy and no longer have the WireTransactions at all, we will
    // promote the ZKProverTransactions to first-class citizens and then they will be saved in the vault as WireTransactions
    // are now.
    fun List<PaddingWrapper<StateAndRef<ContractState>>>.collectUtxoNonces() = mapIndexed { _, it ->
        when (it) {
            is PaddingWrapper.Filler -> {
                // When it is a padded state, the nonce is ALWAYS a zerohash of the algo used for merkle tree leaves
                componentGroupLeafDigestService.zeroHash
            }
            is PaddingWrapper.Original -> {
                // When it is an original state, we look up the tx it points to and collect the nonce for the UTXO it points to.
                val outputTx =
                    zkProverTransactionStorage.getTransaction(it.content.ref.txhash)
                        ?: error("Could not fetch output transaction for StateRef ${it.content.ref}")
                outputTx.tx.merkleTree.componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]!![it.content.ref.index]
            }
        }
    }

    // Collect the nonces for the outputs pointed to by the inputs and references.
    val inputNonces = padded.inputs().collectUtxoNonces()
    val referenceNonces = padded.references().collectUtxoNonces()

    return Witness(this, inputNonces = inputNonces, referenceNonces = referenceNonces)
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
