package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FixedLengthSetSerializerTest : SerializerTest {
    private val set = setOf(1, 2, 3)

    @ParameterizedTest
    @MethodSource("engines")
    fun `Set(Int) must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, set)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Sets of Int must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, set).size shouldBe
            engine.serialize(InstanceSerializer, set + 20).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with Set(Int) must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsSet.serializer(), ContainsSet())
    }

    @Serializable
    data class ContainsSet(
        @Serializable(with = InstanceSerializer::class)
        val innerSet: Set<Int> = setOf(1, 2)
    )

    object InstanceSerializer : FixedLengthSetSerializer<Int>(10, IntSerializer)
}
