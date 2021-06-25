package com.ing.zknotary.gradle.zinc.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.Json
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.crypto.Crypto
import java.io.File

class CircuitConfigurator(
    circuitSources: File,
    configFileName: String
) {
    @Serializable
    data class CircuitConfiguration(val circuit: Circuit, val groups: Groups)

    @Serializable
    data class Circuit(
        val name: String,
        val states: List<State> = listOf(),

        /**
         * All timeouts are in seconds
         */
        val buildTimeout: Int = 120,
        val setupTimeout: Int = 3000,
        val provingTimeout: Int = 300,
        val verificationTimeout: Int = 3
    )

    @Serializable
    data class State(
        val location: String,
        val name: String,
        val notaryKeySchemeCodename: String = ZKFlowNetworkParameters.notaryKeySchemeCodename,
        @SerialName("attachment_constraint") val attachmentConstraint: String =
            AutomaticPlaceholderConstraint::class.qualifiedName!!
    ) {
        init {
            // Verify class' fields.
            try {
                Crypto.findSignatureScheme(notaryKeySchemeCodename)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    """
                        Value `notary_key_scheme_codename` is invalid: $notaryKeySchemeCodename
                        Valid values are: ${Crypto.supportedSignatureSchemes().joinToString(separator = ", ") { it.schemeCodeName}}                      
                    """.trimIndent(),
                    e
                )
            }

            val attachmentClass = try {
                Class.forName(attachmentConstraint)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Value `attachment_constraint` is invalid: $attachmentConstraint", e
                )
            }

            if (!AttachmentConstraint::class.java.isAssignableFrom(attachmentClass)) {
                error(
                    """
                        Value `attachment_constraint` is invalid: $attachmentConstraint
                        $attachmentConstraint must implement ${AttachmentConstraint::class.qualifiedName}                   
                    """.trimIndent()
                )
            }
        }
    }

    @Serializable
    data class Groups(
        @SerialName("attachment_group") val attachmentGroup: Group = Group(),
        @SerialName("input_group") val inputGroup: Group = Group(),
        @SerialName("output_group") val outputGroup: Group = Group(),
        @SerialName("reference_group") val referenceGroup: Group = Group(),
        @SerialName("command_group") val commandGroup: CommandGroup,
        @SerialName("notary_group") val notaryGroup: FixedComponentSizeGroup = FixedComponentSizeGroup(),
        @SerialName("timewindow_group") val timewindowGroup: FixedComponentSizeGroup = FixedComponentSizeGroup(),
        @SerialName("signer_group") val signerGroup: SignersGroup,
    )

    @Serializable(with = GroupSerializer::class)
    data class Group(
        val groupSize: Int = 0,
        val componentSize: Int = 1
    )

    object GroupSerializer : KSerializer<Group> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("group") {
                element<Int>("group_size")
                element<Int>("component_size")
            }

        override fun serialize(encoder: Encoder, value: Group) {
            TODO("Not Supported")
        }

        override fun deserialize(decoder: Decoder): Group =
            decoder.decodeStructure(descriptor) {
                var groupSize = 0
                var componentSize: Int? = null
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> groupSize = decodeIntElement(descriptor, 0)
                        1 -> componentSize = decodeIntElement(descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }

                if (groupSize == 0) { componentSize = 1 }
                if (groupSize != 0 && componentSize == null) {
                    error("Group with `group_size > 0` must specify `component_size`")
                }
                // Invariant: groupSize is Int, componentSize != null
                require(componentSize != null) { "Group rules verification failure." }

                Group(groupSize, componentSize)
            }
    }

    @Serializable
    data class CommandGroup(
        @SerialName("group_size") val groupSize: Int,
        val commands: List<Command>
    )

    @Serializable
    data class Command(
        val name: String,
        @SerialName("component_size") val componentSize: Int
    )

    @Serializable
    data class SignersGroup(
        @SerialName("signer_size") val signerSize: Int,
        @SerialName("signer_list_size") val signerListSize: Int = 1,
        val signerKeySchemeCodename: String = ZKFlowNetworkParameters.signerKeySchemeCodename
    )

    @Serializable
    /** For some groups `component_size`, such as NotaryGroup and TimeWindowGroup, is fixed.
     This class describes this case.
     */
    data class FixedComponentSizeGroup(
        @SerialName("group_size") val groupSize: Int = 0
    )

    val circuitConfiguration: CircuitConfiguration

    init {
        val configPath = circuitSources.resolve(configFileName)
        require(configPath.exists()) { "Configuration file is expected at $configPath" }

        // NOTE: decodeFromString fails silently if JSON is malformed.
        circuitConfiguration = Json.decodeFromString(configPath.readText())

        require(circuitConfiguration.groups.commandGroup.commands.size == 1) {
            "Only a single command per circuit is supported FOR NOW"
        }
    }

    fun generateConstsFile(outputPath: File) = createOutputFile(outputPath.resolve("consts.zn")).appendBytes(
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
// A single command per circuit is currently supported.
// TODO: Support for multiple commands is to be implemented.
const COMMAND_COMPONENT_SIZE: u16 = ${circuitConfiguration.groups.commandGroup.commands.single().componentSize};
const COMMAND_SIGNER_LIST_SIZE: u16 = ${circuitConfiguration.groups.signerGroup.signerListSize};

const INPUT_UTXO_SIZE: u16 = ${circuitConfiguration.groups.inputGroup.componentSize};
const REFERENCE_UTXO_SIZE: u16 = ${circuitConfiguration.groups.referenceGroup.componentSize};
        """.trimIndent().toByteArray()
    )
}
