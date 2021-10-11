package com.ing.zkflow.compilation.zinc.util

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

// TODO: This is obsoleted by ZKTransactionMetadata. It can be removed when ZincPoet is used. Please then also remove all dependencies that
// are no longer necessary, such as Kotlinx JSON serialization (and the gradle plugin too)
class CircuitConfigurator private constructor(val circuitConfiguration: CircuitConfiguration) {
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
        @SerialName("java_class") val javaClass: String,
        @SerialName("zinc_type") val zincType: String,
        val location: String,
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
        @SerialName("attachment_group") val attachmentGroup: FixedComponentSizeGroup = FixedComponentSizeGroup(),
        @SerialName("input_group") val inputGroup: List<StateGroup> = listOf(StateGroup()),
        @SerialName("output_group") val outputGroup: List<StateGroup> = listOf(StateGroup()),
        @SerialName("reference_group") val referenceGroup: List<StateGroup> = listOf(StateGroup()),
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

    @Serializable(with = StateGroupSerializer::class)
    data class StateGroup(
        val javaClass: String = "",
        val stateGroupSize: Int = 0,
        @Transient
        val zincType: String? = null
    ) {
        fun resolve(circuit: Circuit): StateGroup =
            if (stateGroupSize > 0) {
                copy(
                    zincType = circuit.states.singleOrNull { it.javaClass == javaClass }
                        ?.zincType
                        ?: error("Circuit: ${circuit.name}; Java class $javaClass needs to have an associated Zinc type")
                )
            } else {
                this
            }
    }

    object StateGroupSerializer : KSerializer<StateGroup> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("group") {
                element<String>("java_class")
                element<Int>("state_group_size")
            }

        override fun serialize(encoder: Encoder, value: StateGroup) {
            TODO("Not Supported")
        }

        override fun deserialize(decoder: Decoder): StateGroup =
            decoder.decodeStructure(descriptor) {
                var javaClass = ""
                var stateGroupSize = 0
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> javaClass = decodeStringElement(descriptor, 0)
                        1 -> stateGroupSize = decodeIntElement(descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }

                if (stateGroupSize != 0 && javaClass == "") {
                    error("StateGroup with `state_group_size > 0` must specify `java_class`")
                }

                StateGroup(javaClass, stateGroupSize)
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

    companion object {
        fun fromSources(circuitSources: File, configFileName: String): CircuitConfigurator {
            val configPath = circuitSources.resolve(configFileName)
            require(configPath.exists()) { "Configuration file is expected at $configPath" }

            // NOTE: decodeFromString fails silently if JSON is malformed.
            val circuitConfiguration: CircuitConfiguration = Json.decodeFromString(configPath.readText())

            require(circuitConfiguration.groups.commandGroup.commands.size == 1) {
                "Only a single command per circuit is supported FOR NOW"
            }

            // Resolve stateGroups
            return CircuitConfigurator(
                circuitConfiguration.copy(
                    groups = circuitConfiguration.groups.copy(
                        outputGroup = circuitConfiguration.groups.outputGroup.map { it.resolve(circuitConfiguration.circuit) },
                        inputGroup = circuitConfiguration.groups.inputGroup.map { it.resolve(circuitConfiguration.circuit) },
                        referenceGroup = circuitConfiguration.groups.referenceGroup.map { it.resolve(circuitConfiguration.circuit) }
                    )
                )
            )
        }
    }

    fun generateConstsFile(outputPath: File) = createOutputFile(outputPath.resolve("consts.zn")).appendBytes(
        """
const ATTACHMENT_GROUP_SIZE: u16 = ${circuitConfiguration.groups.attachmentGroup.groupSize};
const INPUT_GROUP_SIZE: u16 = ${circuitConfiguration.groups.inputGroup.sumBy { it.stateGroupSize }};
const OUTPUT_GROUP_SIZE: u16 = ${circuitConfiguration.groups.outputGroup.sumBy { it.stateGroupSize }};
const REFERENCE_GROUP_SIZE: u16 = ${circuitConfiguration.groups.referenceGroup.sumBy { it.stateGroupSize }};
const NOTARY_GROUP_SIZE: u16 = ${circuitConfiguration.groups.notaryGroup.groupSize};
const TIMEWINDOW_GROUP_SIZE: u16 = ${circuitConfiguration.groups.timewindowGroup.groupSize};
// This is the size of a single signer and should not contain the Corda SerializationMagic size,
// we use platform_consts::CORDA_SERDE_MAGIC_LENGTH for that
const COMMAND_SIGNER_SIZE: u16 = ${circuitConfiguration.groups.signerGroup.signerSize};

// Component and UTXO sizes cannot be 0, so for not present groups use 1
// A single command per circuit is currently supported.
// TODO: Support for multiple commands is to be implemented.
const COMMAND_COMPONENT_SIZE: u16 = ${circuitConfiguration.groups.commandGroup.commands.single().componentSize};
const COMMAND_SIGNER_LIST_SIZE: u16 = ${circuitConfiguration.groups.signerGroup.signerListSize};
        """.trimIndent().toByteArray()
    )
}
