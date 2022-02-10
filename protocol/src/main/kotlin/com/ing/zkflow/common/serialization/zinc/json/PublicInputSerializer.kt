package com.ing.zkflow.common.serialization.zinc.json

import com.ing.zkflow.common.zkp.PublicInput
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

object PublicInputSerializer {
    fun fromPublicInput(publicInput: PublicInput): String = buildJsonObject {
        putJsonObject("PublicInput") {
            if (publicInput.inputComponentHashes.isNotEmpty())
                put(
                    "input_component_hashes",
                    JsonArray(publicInput.inputComponentHashes.toJsonArray())
                )
            if (publicInput.outputComponentHashes.isNotEmpty())
                put(
                    "output_component_hashes",
                    JsonArray(publicInput.outputComponentHashes.toJsonArray())
                )
            if (publicInput.referenceComponentHashes.isNotEmpty())
                put(
                    "reference_component_hashes",
                    JsonArray(publicInput.referenceComponentHashes.toJsonArray())
                )
            if (publicInput.attachmentComponentHashes.isNotEmpty())
                put(
                    "attachment_component_hashes",
                    JsonArray(publicInput.attachmentComponentHashes.toJsonArray())
                )
            if (publicInput.commandComponentHashes.isNotEmpty())
                put(
                    "command_component_hashes",
                    JsonArray(publicInput.commandComponentHashes.toJsonArray())
                )
            if (publicInput.notaryComponentHashes.isNotEmpty())
                put(
                    "notary_component_hashes",
                    JsonArray(publicInput.notaryComponentHashes.toJsonArray())
                )
            if (publicInput.parametersComponentHashes.isNotEmpty())
                put(
                    "parameters_component_hashes",
                    JsonArray(publicInput.parametersComponentHashes.toJsonArray())
                )
            if (publicInput.timeWindowComponentHashes.isNotEmpty())
                put(
                    "time_window_component_hashes",
                    JsonArray(publicInput.timeWindowComponentHashes.toJsonArray())
                )
            if (publicInput.signersComponentHashes.isNotEmpty())
                put(
                    "signers_component_hashes",
                    JsonArray(publicInput.signersComponentHashes.toJsonArray())
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
        }
    }.toString()
}
