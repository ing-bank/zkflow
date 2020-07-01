package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.states.ZKStateRef
import com.ing.zknotary.common.util.ComponentPadding
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.ComponentGroupEnum

@DeleteForDJVM
fun ZKProverTransaction.prettyPrint(): String {
    val buf = StringBuilder()
    buf.appendln("Transaction:")

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

fun ZKProverTransaction.toZKVerifierTransaction(): ZKVerifierTransaction {
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

    val filler = ComponentPadding.Filler.ZKStateRef(ZKStateRef.empty())
    val componentPadding = ComponentPadding.Builder()
        .inputs(this.padded.inputs.size, filler)
        .outputs(this.padded.outputs.size, filler)
        .references(this.padded.references.size, filler)
        .build()

    return ZKVerifierTransaction(
        this.inputs.map { it.ref },
        this.outputs.map { it.ref },
        this.references.map { it.ref },

        this.notary,
        this.timeWindow,
        this.networkParametersHash,

        this.serializationFactoryService,
        this.componentGroupLeafDigestService,
        this.nodeDigestService,

        this.merkleTree.groupHashes,
        componentNonces,

        componentPadding
    )
}

fun <T> List<T>.pad(n: Int, default: T) = List(n) {
    if (it < size)
        this[it]
    else {
        default
    }
}
