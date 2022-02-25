package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.annotations.corda.Sha256
import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.infra.SecureHashSerializationMetadata
import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer.Companion.SHA256_ALGORITHM
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.SecureHash

@Suppress("MagicNumber") // There is no constant provided by Corda for SHA256 length, but we know it can only be 32.
object SHA256SecureHashSerializationMetadata : SecureHashSerializationMetadata(SHA256_ALGORITHM, 32)
object SHA256SecureHashSerializer : SecureHashSerializer(
    SHA256SecureHashSerializationMetadata.algorithm,
    SHA256SecureHashSerializationMetadata.hashSize
)

open class SecureHashSerializer(private val algorithm: String, private val hashLength: Int) :
    FixedLengthKSerializerWithDefault<SecureHash> {
    companion object {
        val SHA256_ALGORITHM = Sha256::class.simpleName!!
    }

    private val strategy = FixedLengthByteArraySerializer(hashLength)

    override val descriptor = buildClassSerialDescriptor("SecureHash$algorithm") {
        element("bytes", strategy.descriptor)
    }.toFixedLengthSerialDescriptorOrThrow()

    override val default: SecureHash = if (algorithm == SHA256_ALGORITHM) {
        SecureHash.zeroHash
    } else {
        SecureHash.HASH(algorithm, ByteArray(hashLength) { 0 })
    }

    override fun serialize(encoder: Encoder, value: SecureHash) {
        require(value.size == hashLength) {
            "Expected $hashLength bytes during serialization of `$algorithm` hash, got ${value.size}"
        }

        return strategy.serialize(encoder, value.bytes)
    }

    override fun deserialize(decoder: Decoder): SecureHash = with(strategy.deserialize(decoder)) {
        require(size == hashLength) {
            "Expected $hashLength bytes during deserialization of `$algorithm` hash, got $size"
        }

        if (algorithm == SHA256_ALGORITHM) {
            SecureHash.SHA256(bytes = this)
        } else {
            SecureHash.HASH(algorithm, bytes = this)
        }
    }
}
