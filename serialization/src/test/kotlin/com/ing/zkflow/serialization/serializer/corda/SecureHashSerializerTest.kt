package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.annotations.corda.SHA256DigestAlgorithm
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.util.STUB_FOR_TESTING
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SecureHashSerializerTest : SerializerTest {
    companion object {
        @JvmStatic
        fun secureHashSerializerNameFixtures() = listOf(
            Arguments.of(HashAnnotations.Sha256_0, "SecureHashSha256"),
            Arguments.of(HashAnnotations.FancyHash_0, "SecureHashFancyHash")
        )
    }

    @ParameterizedTest
    @MethodSource("secureHashSerializerNameFixtures")
    fun `SecureHashSerializer descriptor names should be camelCase`(serializer: SecureHashSerializer, expectedName: String) {
        serializer.descriptor.serialName shouldBe expectedName
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `SecureHash must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(HashAnnotations.Sha256_0, HashAnnotations.zero.sha256)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `SecureHashes must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(HashAnnotations.Sha256_0, HashAnnotations.zero.sha256).size shouldBe
            engine.serialize(HashAnnotations.Sha256_0, HashAnnotations.one.sha256).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with SecureHashes must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(HashAnnotations.serializer(), HashAnnotations.one)
    }

    @Suppress("ClassName")
    @Serializable
    data class HashAnnotations(
        @Serializable(with = Sha256_0::class) val sha256: SecureHash,
        @Serializable(with = FancyHash_0::class) val fancyHash: SecureHash
    ) {
        object Sha256_0 : SecureHashSerializer(SHA256DigestAlgorithm::class)
        object FancyHash_0 : SecureHashSerializer(FancyHash::class)

        companion object {
            val zero = HashAnnotations(SecureHash.zeroHash, SecureHash.HASH("FANCY_HASH", ByteArray(8) { 0 }))
            val one = HashAnnotations(SecureHash.allOnesHash, SecureHash.HASH("FANCY_HASH", ByteArray(8) { 1 }))
        }
    }

    private class FancyHash : DigestAlgorithm {
        override val algorithm: String = "FANCY_HASH"
        override val digestLength: Int = 8

        override fun digest(bytes: ByteArray): ByteArray {
            STUB_FOR_TESTING()
        }

        companion object {
            val zero = SecureHash.HASH("FANCY_HASH", ByteArray(8) { 0 })
            val one = HashAnnotations(SecureHash.allOnesHash, SecureHash.HASH("FANCY_HASH", ByteArray(8) { 1 }))
        }
    }
}
