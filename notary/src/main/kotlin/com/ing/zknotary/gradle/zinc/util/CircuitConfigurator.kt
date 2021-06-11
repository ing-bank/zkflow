package com.ing.zknotary.gradle.zinc.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class CircuitConfigurator(private val outputPath: File) {

    @Serializable
    data class CircuitConfiguration(val command: Command, val components: Components)

    @Serializable
    data class Command(
        @SerialName("name") val name: String,
        @SerialName("component_size") val componentSize: Int
    )

    @Serializable
    data class Components(
        @SerialName("attachment_group") val attachmentGroup: Component,
        @SerialName("input_group") val inputGroup: Component,
        @SerialName("output_group") val outputGroup: Component,
        @SerialName("reference_group") val referenceGroup: Component,
        @SerialName("notary_group") val notaryGroup: Component,
        @SerialName("timewindow_group") val timewindowGroup: Component,
        @SerialName("signer_group") val signerGroup: Component,
    )

    @Serializable
    data class Component(val quantity: Int, @SerialName("component_size") val componentSize: Int)

    val circuitConfiguration: CircuitConfiguration

    init {
        val configFileContent = outputPath.parentFile.resolve("config.json").readText()
        circuitConfiguration = Json.decodeFromString(configFileContent)
    }

    fun generateConstsFile() {
        val targetFile = createOutputFile(outputPath.resolve("consts.zn"))

        val updatedFileContent =
            constsTemplate().replace(
                "ATTACHMENT_GROUP_SIZE_PLACEHOLDER",
                circuitConfiguration.components.attachmentGroup.quantity.toString()
            )
                .replace("INPUT_GROUP_SIZE_PLACEHOLDER", circuitConfiguration.components.inputGroup.quantity.toString())
                .replace(
                    "OUTPUT_GROUP_SIZE_PLACEHOLDER",
                    circuitConfiguration.components.outputGroup.quantity.toString()
                )
                .replace(
                    "REFERENCE_GROUP_SIZE_PLACEHOLDER",
                    circuitConfiguration.components.referenceGroup.quantity.toString()
                )
                .replace(
                    "NOTARY_GROUP_SIZE_PLACEHOLDER",
                    circuitConfiguration.components.notaryGroup.quantity.toString()
                )
                .replace(
                    "TIMEWINDOW_GROUP_SIZE_PLACEHOLDER",
                    circuitConfiguration.components.timewindowGroup.quantity.toString()
                )
                .replace(
                    "SIGNER_GROUP_SIZE_PLACEHOLDER",
                    circuitConfiguration.components.signerGroup.quantity.toString()
                )

                .replace(
                    "INPUT_COMPONENT_SIZE_PLACEHOLDER",
                    circuitConfiguration.components.inputGroup.componentSize.toString()
                )
                .replace(
                    "OUTPUT_COMPONENT_SIZE_PLACEHOLDER",
                    circuitConfiguration.components.outputGroup.componentSize.toString()
                )
                .replace(
                    "REFERENCE_COMPONENT_SIZE_PLACEHOLDER",
                    circuitConfiguration.components.referenceGroup.componentSize.toString()
                )
                .replace(
                    "SIGNER_COMPONENT_SIZE_PLACEHOLDER",
                    circuitConfiguration.components.signerGroup.componentSize.toString()
                )

                .replace("COMMAND_COMPONENT_SIZE_PLACEHOLDER", circuitConfiguration.command.componentSize.toString())
        targetFile.appendBytes(updatedFileContent.toByteArray())
    }

    private fun constsTemplate(): String {
        return """
//!
//! EXAMPLE DOCUMENT FOR COMPONENT GROUP SIZE CONSTANTS
//! PLEASE UPDATE THE VALUE OF EACH COMPONENT GROUP SIZE WITH THE VALUE OF COMPONENT GROUP SIZES IN ZKDAPP TRANSACTION
//!
//! Sizes of Corda's component groups are fixed to the below values.
//! Corda must produce witness such it contains the expected number of components
//! in each group.
//! See definition of ZKCommandData on the Corda side.
//!
const ATTACHMENT_GROUP_SIZE: u16 = ${"ATTACHMENT_GROUP_SIZE_PLACEHOLDER"};
const INPUT_GROUP_SIZE: u16 = ${"INPUT_GROUP_SIZE_PLACEHOLDER"};
const OUTPUT_GROUP_SIZE: u16 = ${"OUTPUT_GROUP_SIZE_PLACEHOLDER"};
const REFERENCE_GROUP_SIZE: u16 = ${"REFERENCE_GROUP_SIZE_PLACEHOLDER"};
const NOTARY_GROUP_SIZE: u16 = ${"NOTARY_GROUP_SIZE_PLACEHOLDER"};
const TIMEWINDOW_GROUP_SIZE: u16 = ${"TIMEWINDOW_GROUP_SIZE_PLACEHOLDER"};
const SIGNER_GROUP_SIZE: u16 = ${"SIGNER_GROUP_SIZE_PLACEHOLDER"};

// Component and UTXO sizes cannot be 0, so for not present components use 1
const OUTPUT_COMPONENT_SIZE: u16 = ${"OUTPUT_COMPONENT_SIZE_PLACEHOLDER"};
const COMMAND_COMPONENT_SIZE: u16 = ${"COMMAND_COMPONENT_SIZE_PLACEHOLDER"};
const SIGNER_COMPONENT_SIZE: u16 = ${"SIGNER_COMPONENT_SIZE_PLACEHOLDER"};

const INPUT_UTXO_SIZE: u16 = ${"INPUT_COMPONENT_SIZE_PLACEHOLDER"};
const REFERENCE_UTXO_SIZE: u16 = ${"REFERENCE_COMPONENT_SIZE_PLACEHOLDER"};
        """
    }
}
