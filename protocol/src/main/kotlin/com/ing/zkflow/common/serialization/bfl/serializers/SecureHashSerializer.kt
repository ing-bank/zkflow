package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zkflow.common.crypto.ZINC
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.algorithm

object SecureHashSerializer : SurrogateSerializer<SecureHash, SecureHashSurrogate>(
    SecureHashSurrogate.serializer(),
    { SecureHashSurrogate.from(it) }
)

object SecureHashSHA256Serializer : SurrogateSerializer<SecureHash.SHA256, SecureHashSHA256Surrogate>(
    SecureHashSHA256Surrogate.serializer(),
    { SecureHashSHA256Surrogate.from(it) }
)

object SecureHashHASHSerializer : SurrogateSerializer<SecureHash.HASH, SecureHashHASHSurrogate>(
    SecureHashHASHSurrogate.serializer(),
    { SecureHashHASHSurrogate.from(it) }
)

@Suppress("ArrayInDataClass")
@Serializable
data class SecureHashSurrogate(
    val algorithmId: Byte,
    // Hashes expected by Corda must be at most 32 bytes long.
    @FixedLength([BYTES_SIZE])
    val bytes: ByteArray
) : Surrogate<SecureHash> {
    override fun toOriginal(): SecureHash {
        return when (val alg = SecureHashSupportedAlgorithm.fromByte(algorithmId).algorithm) {
            SecureHash.SHA2_256 -> SecureHash.SHA256(bytes)
            else -> SecureHash.HASH(alg, bytes)
        }
    }

    companion object {
        const val BYTES_SIZE = 32
        fun from(original: SecureHash): SecureHashSurrogate {
            val algorithmId = SecureHashSupportedAlgorithm.fromAlgorithm(original.algorithm).id
            return SecureHashSurrogate(algorithmId, original.bytes)
        }
    }
}

@Suppress("ArrayInDataClass")
@Serializable
data class SecureHashSHA256Surrogate(
    val algorithmId: Byte,
    // Hashes expected by Corda must be at most 32 bytes long.
    @FixedLength([BYTES_SIZE])
    val bytes: ByteArray
) : Surrogate<SecureHash.SHA256> {
    override fun toOriginal() = SecureHash.SHA256(bytes)

    companion object {
        const val BYTES_SIZE = 32
        fun from(original: SecureHash.SHA256) = SecureHashSHA256Surrogate(0, original.bytes)
    }
}

@Suppress("ArrayInDataClass")
@Serializable
data class SecureHashHASHSurrogate(
    val algorithmId: Byte,
    // Hashes expected by Corda must be at most 32 bytes long.
    @FixedLength([BYTES_SIZE])
    val bytes: ByteArray
) : Surrogate<SecureHash.HASH> {
    override fun toOriginal() = SecureHash.HASH(SecureHashSupportedAlgorithm.fromByte(algorithmId).algorithm, bytes)

    companion object {
        const val BYTES_SIZE = 32
        fun from(original: SecureHash.HASH) = SecureHashHASHSurrogate(
            SecureHashSupportedAlgorithm.fromAlgorithm(original.algorithm).id,
            original.bytes
        )
    }
}

/**
 * This class lists the supported hashing algorithms for serialization, and
 * defines a mapping for them.
 */
enum class SecureHashSupportedAlgorithm(val id: Byte, val algorithm: String) {
    SHA_256(0, SecureHash.SHA2_256),
    ZINC(1, SecureHash.ZINC);

    companion object {
        fun fromByte(id: Byte): SecureHashSupportedAlgorithm = values()
            .find { it.id == id }
            ?: throw IllegalArgumentException("No algorithm found for id: $id")

        fun fromAlgorithm(algorithm: String): SecureHashSupportedAlgorithm = values()
            .find { it.algorithm == algorithm }
            ?: throw IllegalArgumentException("No algorithm found for: $algorithm")
    }
}
