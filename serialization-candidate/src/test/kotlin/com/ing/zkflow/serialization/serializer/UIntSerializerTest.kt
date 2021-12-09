package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UIntSerializerTest : SerializerTest {
    private val int: UInt = 1u

    @ParameterizedTest
    @MethodSource("engines")
    fun `UInt must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(UIntSerializer, int)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `UInts must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(UIntSerializer, int).size shouldBe
            engine.serialize(UIntSerializer, int + 1u).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List UInt must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsUInts.serializer(), ContainsUInts())
    }

    @Serializable
    data class ContainsUInts(
        @Serializable(with = UIntSerializer::class)
        val uint: UInt = 1U,

        @Serializable(with = UInt_0::class)
        val ints: List<UInt?> = listOf(1U, null)
    ) {
        object UInt_0 : FixedLengthListSerializer<UInt?>(5, UInt_1)
        object UInt_1 : NullableSerializer<UInt>(UInt_2)
        object UInt_2 : WrappedKSerializerWithDefault<UInt>(UIntSerializer)
    }
}
