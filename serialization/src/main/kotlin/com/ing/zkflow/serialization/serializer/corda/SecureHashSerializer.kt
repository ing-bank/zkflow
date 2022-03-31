package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import com.ing.zkflow.util.snakeToCamelCase
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.internal.DigestAlgorithmFactory
import kotlin.reflect.KClass

object SHA256SecureHashSerializer : SecureHashSerializer(DigestAlgorithmFactory.create(SecureHash.SHA2_256))

open class SecureHashSerializer(private val digestAlgorithm: DigestAlgorithm) : FixedLengthKSerializerWithDefault<SecureHash> {
    constructor(digestAlgorithmKClass: KClass<out DigestAlgorithm>) : this(
        // Corda requires DigestAlgorithm implementations to have an empty constructor,
        // see [DigestAlgorithmFactory.CustomAlgorithmFactory].
        digestAlgorithmKClass
            .java
            .asSubclass(DigestAlgorithm::class.java)
            .getConstructor()
            .newInstance()
    )

    private val strategy = FixedLengthByteArraySerializer(digestAlgorithm.digestLength)

    /**
     * Remove characters not accepted by Zinc.
     */
    private val algorithmIdentifier = digestAlgorithm.algorithm.snakeToCamelCase(capitalize = true)

    override val descriptor = buildClassSerialDescriptor("SecureHash$algorithmIdentifier") {
        element("bytes", strategy.descriptor)
    }.toFixedLengthSerialDescriptorOrThrow()

    override val default: SecureHash = if (digestAlgorithm.algorithm == SecureHash.SHA2_256) {
        SecureHash.zeroHash
    } else {
        SecureHash.HASH(digestAlgorithm.algorithm, ByteArray(digestAlgorithm.digestLength) { 0 })
    }

    override fun serialize(encoder: Encoder, value: SecureHash) {
        require(value.size == digestAlgorithm.digestLength) {
            "Expected ${digestAlgorithm.digestLength} bytes during serialization of `${digestAlgorithm.algorithm}` hash, got ${value.size}"
        }

        return strategy.serialize(encoder, value.bytes)
    }

    override fun deserialize(decoder: Decoder): SecureHash = with(strategy.deserialize(decoder)) {
        require(size == digestAlgorithm.digestLength) {
            "Expected ${digestAlgorithm.digestLength} bytes during deserialization of `${digestAlgorithm.algorithm}` hash, got $size"
        }

        if (digestAlgorithm.algorithm == SecureHash.SHA2_256) {
            SecureHash.SHA256(bytes = this)
        } else {
            SecureHash.HASH(digestAlgorithm.algorithm, bytes = this)
        }
    }
}
