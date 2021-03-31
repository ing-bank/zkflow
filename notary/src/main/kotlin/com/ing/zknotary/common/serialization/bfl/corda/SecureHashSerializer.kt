package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.SecureHash

open class SealedSecureHashSerializer<T : SecureHash> : KSerializer<T> {
    private val strategy = SecureHashSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): T {
        @Suppress("UNCHECKED_CAST")
        return decoder.decodeSerializableValue(strategy).toOriginal() as? T
            ?: error("Cannot deserialize SecureHash")
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(strategy, SecureHashSurrogate.from(value))
    }
}

object SecureHashSerializer : KSerializer<SecureHash> by SealedSecureHashSerializer()
object SecureHashSHA256Serializer : KSerializer<SecureHash.SHA256> by SealedSecureHashSerializer()
object SecureHashHASHSerializer : KSerializer<SecureHash.HASH> by SealedSecureHashSerializer()

@Suppress("ArrayInDataClass")
@Serializable
data class SecureHashSurrogate(
    @FixedLength([20])
    val algorithm: String,
    // Hashes expected by Corda must be at most 32 bytes long.
    @FixedLength([32])
    val bytes: ByteArray
) {
    fun toOriginal() = when (algorithm) {
        SHA256_algo -> SecureHash.SHA256(bytes)
        else -> SecureHash.HASH(algorithm, bytes)
    }

    companion object {
        const val SHA256_algo = "SHA256"
        fun from(original: SecureHash): SecureHashSurrogate {
            val algorithm = when (original) {
                is SecureHash.SHA256 -> SHA256_algo
                is SecureHash.HASH -> original.algorithm
            }

            return SecureHashSurrogate(algorithm, original.bytes)
        }
    }
}
