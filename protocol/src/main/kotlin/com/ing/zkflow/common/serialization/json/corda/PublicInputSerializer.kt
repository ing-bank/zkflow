package com.ing.zkflow.common.serialization.json.corda

import com.ing.zkflow.common.zkp.PublicInput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.SecureHash

object PublicInputSerializer : KSerializer<PublicInput> {
    @Serializable
    @SerialName("PublicInput")
    private data class PublicInputSurrogate(
        @SerialName("input_hashes") val inputHashes: List<@Serializable(with = SecureHashSerializer::class) SecureHash>,
        @SerialName("reference_hashes") val referenceHashes: List<@Serializable(with = SecureHashSerializer::class) SecureHash>,
        @SerialName("transaction_id") val transactionId: @Serializable(with = SecureHashSerializer::class) SecureHash
    ) {
        companion object {
            fun fromPublicInput(publicInput: PublicInput): PublicInputSurrogate {
                return PublicInputSurrogate(
                    transactionId = publicInput.transactionId,
                    inputHashes = publicInput.inputHashes,
                    referenceHashes = publicInput.referenceHashes
                )
            }
        }
    }

    override val descriptor: SerialDescriptor = PublicInputSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: PublicInput) {
        val surrogate = PublicInputSurrogate.fromPublicInput(value)
        encoder.encodeSerializableValue(PublicInputSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): PublicInput = throw NotImplementedError()
}
