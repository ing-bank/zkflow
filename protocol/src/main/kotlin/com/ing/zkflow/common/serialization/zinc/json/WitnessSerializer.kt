package com.ing.zkflow.common.serialization.zinc.json

import com.ing.zkflow.common.zkp.Witness
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

@Suppress("ComplexMethod", "LongMethod")
object WitnessSerializer {
    fun fromWitness(witness: Witness): String = buildJsonObject {
        putJsonObject("input") {
            if (witness.outputsGroup.isNotEmpty())
                putJsonObject("outputs") {
                    witness.outputsGroup.forEach {
                        put(it.name, JsonArray(it.serializedData.toUnsignedBitString()))
                    }
                }
            if (witness.commandsGroup.isNotEmpty())
                put(
                    "commands",
                    JsonArray(witness.commandsGroup.toJsonArray())
                )
            if (witness.attachmentsGroup.isNotEmpty())
                put(
                    "attachments",
                    JsonArray(witness.attachmentsGroup.toJsonArray())
                )
            if (witness.notaryGroup.isNotEmpty())
                put(
                    "notary",
                    JsonArray(witness.notaryGroup.toJsonArray())
                )
            if (witness.timeWindowGroup.isNotEmpty())
                put(
                    "time_window",
                    JsonArray(witness.timeWindowGroup.toJsonArray())
                )
            if (witness.signersGroup.isNotEmpty())
                put(
                    "signers",
                    JsonArray(witness.signersGroup.toJsonArray())
                )
            if (witness.parametersGroup.isNotEmpty())
                put(
                    "parameters",
                    JsonArray(witness.parametersGroup.toJsonArray())
                )

            put("privacy_salt", JsonArray(witness.privacySalt.bytes.toUnsignedBitString()))

            if (witness.serializedInputUtxos.isNotEmpty()) {
                putJsonObject("serialized_input_utxos") {
                    witness.serializedInputUtxos.forEach {
                        put(it.name, JsonArray(it.serializedData.toUnsignedBitString()))
                    }
                }
                put(
                    "input_nonces",
                    JsonArray(witness.inputUtxoNonces.toJsonArray())
                )
            }
            if (witness.serializedReferenceUtxos.isNotEmpty()) {
                putJsonObject("serialized_reference_utxos") {
                    witness.serializedReferenceUtxos.forEach {
                        put(it.name, JsonArray(it.serializedData.toUnsignedBitString()))
                    }
                }
                put(
                    "reference_nonces",
                    JsonArray(witness.referenceUtxoNonces.toJsonArray())
                )
            }
        }
    }.toString()
}
