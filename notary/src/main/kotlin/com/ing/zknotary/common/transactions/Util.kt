package com.ing.zknotary.common.transactions

import net.corda.core.DeleteForDJVM
import java.util.function.Predicate

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
    addComponentList(buf, "COMMANDS", commands)
    addComponentList(buf, "ATTACHMENT HASHES", attachments)

    if (networkParametersHash != null) {
        buf.appendln(" - PARAMETERS HASH:  $networkParametersHash")
    }
    return buf.toString()
}

/**
 * Build filtered transaction using provided filtering functions.
 */
fun ZKProverTransaction.toZKVerifierTransaction(filtering: Predicate<Any>): ZKVerifierTransaction =
    ZKVerifierTransaction.fromZKProverTransaction(this, filtering)

fun ZKProverTransaction.toZKVerifierTransactionSimplified(): ZKVerifierTransactionSimplified =
    ZKVerifierTransactionSimplified.fromZKProverTransaction(this)
