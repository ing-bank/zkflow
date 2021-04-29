package com.ing.zknotary.common.serialization.json.corda

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.zkp.Witness
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.SecureHash

object WitnessSerializer : KSerializer<Witness> {
    @Serializable
    @SerialName("Witness")
    private data class WitnessSurrogate(
        @SerialName("inputs") val inputsGroup: List<List<String>>,
        @SerialName("outputs") val outputsGroup: List<List<String>>,
        @SerialName("commands") val commandsGroup: List<List<String>>,
        @SerialName("attachments") val attachmentsGroup: List<List<String>>,
        @SerialName("notary") val notaryGroup: List<List<String>>,
        @SerialName("time_window") val timeWindowGroup: List<List<String>>,
        @SerialName("signers") val signersGroup: List<List<String>>,
        @SerialName("references") val referencesGroup: List<List<String>>,
        @SerialName("parameters") val parametersGroup: List<List<String>>,

        @SerialName("privacy_salt") val privacySalt: List<String>,

        @SerialName("serialized_input_utxos") val serializedInputUtxos: List<List<String>>,
        @SerialName("serialized_reference_utxos") val serializedReferenceUtxos: List<List<String>>,

        @SerialName("input_nonces") val inputNonces: List<@Serializable(with = SecureHashSerializer::class) SecureHash>,
        @SerialName("reference_nonces") val referenceNonces: List<@Serializable(with = SecureHashSerializer::class) SecureHash>
    )

    @Serializable
    private data class WrappedSurrogate(val witness: WitnessSurrogate) {
        companion object {
            fun fromWitness(witness: Witness): WrappedSurrogate {
                return WrappedSurrogate(
                    WitnessSurrogate(
                        inputsGroup = witness.inputsGroup.map { it.map { byte -> byte.asUnsigned().toString() } },
                        outputsGroup = witness.outputsGroup.map { it.map { byte -> byte.asUnsigned().toString() } },
                        commandsGroup = witness.commandsGroup.map { it.map { byte -> byte.asUnsigned().toString() } },
                        attachmentsGroup = witness.attachmentsGroup.map { it.map { byte -> byte.asUnsigned().toString() } },
                        notaryGroup = witness.notaryGroup.map { it.map { byte -> byte.asUnsigned().toString() } },
                        timeWindowGroup = witness.timeWindowGroup.map { it.map { byte -> byte.asUnsigned().toString() } },
                        signersGroup = witness.signersGroup.map { it.map { byte -> byte.asUnsigned().toString() } },
                        referencesGroup = witness.referencesGroup.map { it.map { byte -> byte.asUnsigned().toString() } },
                        parametersGroup = witness.parametersGroup.map { it.map { byte -> byte.asUnsigned().toString() } },
                        privacySalt = witness.privacySalt.bytes.asList().map { byte -> byte.asUnsigned().toString() },
                        serializedInputUtxos = witness.serializedInputUtxos.map { it.map { byte -> byte.asUnsigned().toString() } },
                        serializedReferenceUtxos = witness.serializedReferenceUtxos.map { it.map { byte -> byte.asUnsigned().toString() } },
                        inputNonces = witness.inputUtxoNonces,
                        referenceNonces = witness.referenceUtxoNonces
                    )
                )
            }
        }
    }

    override val descriptor: SerialDescriptor = WrappedSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Witness) {
        val surrogate = WrappedSurrogate.fromWitness(value)
        encoder.encodeSerializableValue(WrappedSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Witness = throw NotImplementedError()
}
