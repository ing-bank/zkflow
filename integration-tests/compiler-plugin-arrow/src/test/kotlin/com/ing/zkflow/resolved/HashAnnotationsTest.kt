package com.ing.zkflow.resolved

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@kotlinx.serialization.Serializable
data class HashAnnotations(
    @kotlinx.serialization.Serializable(with = Sha256_0::class) val sha256: @kotlinx.serialization.Contextual SecureHash = SecureHash.zeroHash,
    @kotlinx.serialization.Serializable(with = FancyHash_0::class) val fancyHash: @kotlinx.serialization.Contextual SecureHash = SecureHash.HASH("FancyHash", ByteArray(8) { 0 })
) {
    object Sha256_0 : com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer("Sha256", 32)
    object FancyHash_0 : com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer("FancyHash", 8)
}

class HashAnnotationsTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `HashAnnotations make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.HashAnnotations.serializer(),
            com.ing.zkflow.annotated.HashAnnotations()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `HashAnnotations generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.HashAnnotations.serializer(),
            com.ing.zkflow.annotated.HashAnnotations()
        ) shouldBe
            engine.serialize(HashAnnotations.serializer(), HashAnnotations())
    }
}
