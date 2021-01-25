package com.ing.zknotary.common.input

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun composeTempCircuit(sourceCircuit: String, sharedModules: Array<String>) {
    for (module in sharedModules) {
        val modulePath = Paths.get(module)
        Files.copy(modulePath, Paths.get(sourceCircuit, "src", modulePath.fileName.toString()), StandardCopyOption.REPLACE_EXISTING)
    }
}

fun toBigDecimalJSON(sign: Byte, integer: ByteArray, fraction: ByteArray) =
    "{\"sign\": \"$sign\", \"integer\": [${integer.joinToString { "\"${it}\"" }}], \"fraction\": ${fraction.toPrettyString()}}"

fun toDoubleJSON(sign: Byte, exponent: Short, magnitude: Long) =
    "{\"exponent\": \"${exponent}\",\"magnitude\": \"${magnitude}\",\"sign\": \"${sign}\"}"

fun ByteArray.toPrettyString() = "[${this.joinToString { "\"${it}\"" }}]"
