package com.ing.zknotary.gradle.zinc.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class CircuitConfigurator(private val outputPath: File) {

    @Serializable
    data class CircuitConfiguration(val command: Command, val groups: Groups)

    @Serializable
    data class Command(
        @SerialName("name") val name: String,
        @SerialName("component_size") val componentSize: Int
    )

    @Serializable
    data class Groups(
        @SerialName("attachment_group") val attachmentGroup: Group = Group(),
        @SerialName("input_group") val inputGroup: Group = Group(),
        @SerialName("output_group") val outputGroup: Group = Group(),
        @SerialName("reference_group") val referenceGroup: Group = Group(),
        @SerialName("notary_group") val notaryGroup: Group = Group(),
        @SerialName("timewindow_group") val timewindowGroup: Group = Group(),
        @SerialName("signer_group") val signerGroup: SignersGroup,
    )

    @Serializable
    data class Group(
        @SerialName("group_size")val groupSize: Int = 0,
        @SerialName("component_size") val componentSize: Int = 1
    )

    @Serializable
    data class SignersGroup(
        @SerialName("signer_size")val signerSize: Int,
        @SerialName("signer_list_size") val signerListSize: Int = 1
    )

    val circuitConfiguration: CircuitConfiguration

    init {
        val configFileContent = outputPath.parentFile.resolve("config.json").readText()
        circuitConfiguration = Json.decodeFromString(configFileContent)
    }

    fun generateConstsFile() = createOutputFile(outputPath.resolve("consts.zn")).appendBytes(
        """
const ATTACHMENT_GROUP_SIZE: u16 = ${circuitConfiguration.groups.attachmentGroup.groupSize};
const INPUT_GROUP_SIZE: u16 = ${circuitConfiguration.groups.inputGroup.groupSize};
const OUTPUT_GROUP_SIZE: u16 = ${circuitConfiguration.groups.outputGroup.groupSize};
const REFERENCE_GROUP_SIZE: u16 = ${circuitConfiguration.groups.referenceGroup.groupSize};
const NOTARY_GROUP_SIZE: u16 = ${circuitConfiguration.groups.notaryGroup.groupSize};
const TIMEWINDOW_GROUP_SIZE: u16 = ${circuitConfiguration.groups.timewindowGroup.groupSize};
// This is the size of a single signer and should not contain the Corda SerializationMagic size,
// we use platform_consts::CORDA_SERDE_MAGIC_LENGTH for that
const COMMAND_SIGNER_SIZE: u16 = ${circuitConfiguration.groups.signerGroup.signerSize};

// Component and UTXO sizes cannot be 0, so for not present groups use 1
const OUTPUT_COMPONENT_SIZE: u16 = ${circuitConfiguration.groups.outputGroup.componentSize};
const COMMAND_COMPONENT_SIZE: u16 = ${circuitConfiguration.command.componentSize};
const COMMAND_SIGNER_LIST_SIZE: u16 = ${circuitConfiguration.groups.signerGroup.signerListSize};

const INPUT_UTXO_SIZE: u16 = ${circuitConfiguration.groups.inputGroup.componentSize};
const REFERENCE_UTXO_SIZE: u16 = ${circuitConfiguration.groups.referenceGroup.componentSize};
        """.trimIndent().toByteArray()
    )
}
