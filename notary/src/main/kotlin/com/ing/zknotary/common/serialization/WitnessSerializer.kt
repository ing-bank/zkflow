package com.ing.zknotary.common.serialization

import com.ing.zknotary.common.zkp.Witness
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.contracts.PrivacySalt
import net.corda.core.crypto.SecureHash

@ExperimentalSerializationApi
object WitnessSerializer : KSerializer<Witness> {
    @Serializable
    @SerialName("Witness")
    private data class WitnessSurrogate(
        // TODO: Should all bytes of these componentgroups be made unsigned?
        val inputsGroup: List<ByteArray>,
        val outputsGroup: List<ByteArray>,
        val commandsGroup: List<ByteArray>,
        val attachmentsGroup: List<ByteArray>,
        val notaryGroup: List<ByteArray>,
        val timeWindowGroup: List<ByteArray>,
        val signersGroup: List<ByteArray>,
        val referencesGroup: List<ByteArray>,
        val parametersGroup: List<ByteArray>,

        val privacySalt: @Serializable(with = PrivacySaltSerializer::class) PrivacySalt,

        // TODO: add serializers for the below types and uncomment these lines
//        val inputStates: List<StateAndRef<ContractState>>,
//        val referenceStates: List<StateAndRef<ContractState>>,

        val inputNonces: List<@Serializable(with = SecureHashSerializer::class) SecureHash>,
        val referenceNonces: List<@Serializable(with = SecureHashSerializer::class) SecureHash>
    ) {
        companion object {
            fun fromWitness(witness: Witness): WitnessSurrogate {
                return WitnessSurrogate(
                    inputsGroup = witness.inputsGroup,
                    outputsGroup = witness.outputsGroup,
                    commandsGroup = witness.commandsGroup,
                    attachmentsGroup = witness.attachmentsGroup,
                    notaryGroup = witness.notaryGroup,
                    timeWindowGroup = witness.timeWindowGroup,
                    signersGroup = witness.signersGroup,
                    referencesGroup = witness.referencesGroup,
                    parametersGroup = witness.parametersGroup,
                    privacySalt = witness.privacySalt,
//                    inputStates = witness.inputStates,
//                    referenceStates = witness.referenceStates,
                    inputNonces = witness.inputNonces,
                    referenceNonces = witness.referenceNonces
                )
            }
        }
    }

    override val descriptor: SerialDescriptor = WitnessSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Witness) {
        val surrogate = WitnessSurrogate.fromWitness(value)
        encoder.encodeSerializableValue(WitnessSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Witness = throw NotImplementedError()
}
