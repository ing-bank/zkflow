package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.Algorithm
import com.ing.zkflow.annotations.corda.SHA256
import com.ing.zkflow.annotations.corda.SHA256DigestAlgorithm
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class HashAnnotationsTest : SerializerTest {
    // Setup
    @ZKP
    data class HashAnnotations(
        val sha256: @SHA256 SecureHash = SecureHash.zeroHash,
        val fancyHash: @FancyHash SecureHash = SecureHash.HASH("FancyHash", ByteArray(FancyDigestAlgorithm.DIGEST_LENGTH) { 0 })
    )

    @Target(AnnotationTarget.TYPE)
    @Algorithm(FancyDigestAlgorithm::class)
    annotation class FancyHash

    class FancyDigestAlgorithm : DigestAlgorithm {
        override val algorithm = "FancyHash"
        override val digestLength = DIGEST_LENGTH

        override fun digest(bytes: ByteArray) = placeholder()
        override fun componentDigest(bytes: ByteArray) = placeholder()
        override fun nonceDigest(bytes: ByteArray) = placeholder()

        private fun placeholder(): Nothing {
            error("`${this::class.qualifiedName}` is a placeholder DigestAlgorithm only used in tests.")
        }

        companion object {
            const val DIGEST_LENGTH = 8
        }
    }

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class HashAnnotationsResolved(
        @Serializable(with = Sha256_0::class) val sha256: @Contextual SecureHash = SecureHash.zeroHash,
        @Serializable(with = FancyHash_0::class) val fancyHash: @Contextual SecureHash = SecureHash.HASH("FancyHash", ByteArray(8) { 0 })
    ) {
        object Sha256_0 : SecureHashSerializer(SHA256DigestAlgorithm::class)
        object FancyHash_0 : SecureHashSerializer(FancyDigestAlgorithm::class)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `HashAnnotations makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(HashAnnotationsTest_HashAnnotations_Serializer, HashAnnotations())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `HashAnnotations generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            HashAnnotationsResolved.serializer(),
            HashAnnotationsResolved()
        ) shouldBe
            engine.serialize(HashAnnotationsTest_HashAnnotations_Serializer, HashAnnotations())
    }
}
