package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SecureHashSerializerTest : SerializerTest {
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
        object Sha256_0 : SecureHashSerializer("Sha256", 32)
        object FancyHash_0 : SecureHashSerializer("FancyHash", 8)

        companion object {
            val zero = HashAnnotations(SecureHash.zeroHash, SecureHash.HASH("FancyHash", ByteArray(8) { 0 }))
            val one = HashAnnotations(SecureHash.allOnesHash, SecureHash.HASH("FancyHash", ByteArray(8) { 1 }))
        }
    }
}
