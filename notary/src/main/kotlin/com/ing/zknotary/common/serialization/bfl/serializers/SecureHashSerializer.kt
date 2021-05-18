package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.common.crypto.ZINC
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.algorithm

class SealedSecureHashSerializer<T : SecureHash> : KSerializer<T> {
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
    val algorithmId: Byte,
    // Hashes expected by Corda must be at most 32 bytes long.
    @FixedLength([BYTES_SIZE])
    val bytes: ByteArray
) {
    fun toOriginal(): SecureHash {
        return when (val alg = SecureHashSupportedAlgorithm.fromByte(algorithmId).algorithm) {
            SecureHash.SHA2_256 -> SecureHash.SHA256(bytes)
            else -> SecureHash.HASH(alg, bytes)
        }
    }

    companion object {
        const val BYTES_SIZE = 64
        fun from(original: SecureHash): SecureHashSurrogate {
            val algorithmId = SecureHashSupportedAlgorithm.fromAlgorithm(original.algorithm).id
            return SecureHashSurrogate(algorithmId, original.bytes)
        }
    }
}

/**
 * This class lists the supported hashing algorithms for serialization, and
 * defines a mapping for them.
 */
enum class SecureHashSupportedAlgorithm(val id: Byte, val algorithm: String) {
    SHA_256(0, SecureHash.SHA2_256),
    SHA_384(1, SecureHash.SHA2_384),
    SHA_512(2, SecureHash.SHA2_512),
    ZINC(3, SecureHash.ZINC);

    companion object {
        fun fromByte(id: Byte): SecureHashSupportedAlgorithm = values()
            .find { it.id == id }
            ?: throw IllegalArgumentException("No algorithm found for id: $id")

        fun fromAlgorithm(algorithm: String): SecureHashSupportedAlgorithm = values()
            .find { it.algorithm == algorithm }
            ?: throw IllegalArgumentException("No algorithm found for: $algorithm")
    }
}
