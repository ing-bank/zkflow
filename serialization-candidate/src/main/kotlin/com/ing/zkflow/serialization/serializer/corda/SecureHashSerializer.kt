package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.annotations.corda.Sha256
import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.SecureHash

open class SecureHashSerializer(private val algorithm: String, private val hashLength: Int) :
    KSerializerWithDefault<SecureHash> {
    private val strategy = FixedLengthByteArraySerializer(hashLength)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SecureHash$algorithm") {
        element("bytes", strategy.descriptor)
    }

    companion object {
        val sha256Algorithm = Sha256::class.simpleName
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
