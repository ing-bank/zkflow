package com.ing.zkflow.common.serialization.zinc.json

import com.ing.zkflow.common.zkp.PublicInput
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject

object PublicInputSerializer {
    fun fromPublicInput(publicInput: PublicInput): String = buildJsonObject {
        if (publicInput.outputComponentHashes.isNotEmpty())
            put(
                "output_hashes",
                JsonArray(publicInput.outputComponentHashes.toJsonArray())
            )
        if (publicInput.attachmentComponentHashes.isNotEmpty())
            put(
                "attachment_hashes",
                JsonArray(publicInput.attachmentComponentHashes.toJsonArray())
            )
        if (publicInput.commandComponents.isNotEmpty())
            put(
                "command_components",
                JsonArray(publicInput.commandComponents.toJsonArray())
            )
        if (publicInput.notaryComponents.isNotEmpty())
            put(
                "notary_components",
                JsonArray(publicInput.notaryComponents.toJsonArray())
            )
        if (publicInput.parametersComponents.isNotEmpty())
            put(
                "parameters_components",
                JsonArray(publicInput.parametersComponents.toJsonArray())
            )
        if (publicInput.timeWindowComponents.isNotEmpty())
            put(
                "time_window_components",
                JsonArray(publicInput.timeWindowComponents.toJsonArray())
            )
        if (publicInput.signersComponents.isNotEmpty())
            put(
                "signers_components",
                JsonArray(publicInput.signersComponents.toJsonArray())
            )
        if (publicInput.inputUtxoHashes.isNotEmpty())
            put(
                "input_utxo_hashes",
                JsonArray(publicInput.inputUtxoHashes.toJsonArray())
            )
        if (publicInput.referenceUtxoHashes.isNotEmpty())
            put(
                "reference_utxo_hashes",
                JsonArray(publicInput.referenceUtxoHashes.toJsonArray())
            )
        if (publicInput.inputStateRefs.isNotEmpty())
            put(
                "input_stateref_components",
                JsonArray(publicInput.inputStateRefs.toJsonArray())
            )
    }.toString()
}
