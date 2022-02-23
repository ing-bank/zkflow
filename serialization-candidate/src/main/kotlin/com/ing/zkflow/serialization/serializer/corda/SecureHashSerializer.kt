package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.annotations.corda.HashSize
import com.ing.zkflow.annotations.corda.Sha256
import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.SecureHash

// TODO accept hash annotation class
open class SecureHashSerializer(private val algorithm: String, private val hashLength: Int) :
    FixedLengthKSerializerWithDefault<SecureHash> {
    private val strategy = FixedLengthByteArraySerializer(hashLength)

    override val descriptor = buildClassSerialDescriptor("SecureHash$algorithm") {
        element("bytes", strategy.descriptor)
    }.toFixedLengthSerialDescriptorOrThrow()

    companion object {
        val sha256Algorithm = Sha256::class.simpleName!!
        val sha256HashLength = (Sha256::class.annotations.single { it is HashSize } as HashSize).size
    }

    override val default: SecureHash = if (algorithm == sha256Algorithm) {
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

        if (algorithm == sha256Algorithm) {
            SecureHash.SHA256(bytes = this)
        } else {
            SecureHash.HASH(algorithm, bytes = this)
        }
    }
}
