package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.util.PaddingWrapper
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
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
    val componentNonces = this.merkleTree.componentNonces.filterKeys {
        it in listOf(
            ComponentGroupEnum.INPUTS_GROUP.ordinal,
            ComponentGroupEnum.OUTPUTS_GROUP.ordinal,
            ComponentGroupEnum.REFERENCES_GROUP.ordinal,
            ComponentGroupEnum.NOTARY_GROUP.ordinal,
            ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
            ComponentGroupEnum.PARAMETERS_GROUP.ordinal
        )
    }

    val transactionStateFiller = this.padded.paddingConfiguration.filler(ComponentGroupEnum.OUTPUTS_GROUP)!!
    val stateRefFiller = ComponentPaddingConfiguration.Filler.StateRef(StateRef(componentGroupLeafDigestService.zeroHash, 0))
    val secureHashFiller = ComponentPaddingConfiguration.Filler.SecureHash(componentGroupLeafDigestService.zeroHash)
    val componentPadding = ComponentPaddingConfiguration.Builder()
        .inputs(this.padded.sizeOf(ComponentGroupEnum.INPUTS_GROUP), stateRefFiller)
        .outputs(this.padded.sizeOf(ComponentGroupEnum.OUTPUTS_GROUP), transactionStateFiller)
        .references(this.padded.sizeOf(ComponentGroupEnum.REFERENCES_GROUP), stateRefFiller)
        .attachments(this.padded.sizeOf(ComponentGroupEnum.ATTACHMENTS_GROUP), secureHashFiller)
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
