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
        if (publicInput.commandComponentHashes.isNotEmpty())
            put(
                "command_hashes",
                JsonArray(publicInput.commandComponentHashes.toJsonArray())
            )
        if (publicInput.notaryComponentHashes.isNotEmpty())
            put(
                "notary_hashes",
                JsonArray(publicInput.notaryComponentHashes.toJsonArray())
            )
        if (publicInput.parametersComponentHashes.isNotEmpty())
            put(
                "parameters_hashes",
                JsonArray(publicInput.parametersComponentHashes.toJsonArray())
            )
        if (publicInput.timeWindowComponentHashes.isNotEmpty())
            put(
                "time_window_hashes",
                JsonArray(publicInput.timeWindowComponentHashes.toJsonArray())
            )
        if (publicInput.signersComponentHashes.isNotEmpty())
            put(
                "signers_hashes",
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
    }.toString()
}
