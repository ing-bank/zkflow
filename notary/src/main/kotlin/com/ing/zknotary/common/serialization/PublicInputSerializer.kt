package com.ing.zknotary.common.serialization

import com.ing.zknotary.common.zkp.PublicInput
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.SecureHash

@ExperimentalSerializationApi
object PublicInputSerializer : KSerializer<PublicInput> {
    @Serializable
    @SerialName("PublicInput")
    private data class PublicInputSurrogate(
        val transactionId: @Serializable(with = SecureHashSerializer::class) SecureHash,
        val inputHashes: List<@Serializable(with = SecureHashSerializer::class) SecureHash>,
        val referenceHashes: List<@Serializable(with = SecureHashSerializer::class) SecureHash>
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
