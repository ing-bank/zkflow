package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SerializerWithDefaultTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with defaulted serializer must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsDefaultedInt.serializer(), ContainsDefaultedInt())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Classes with defaulted serializer must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(ContainsDefaultedInt.serializer(), ContainsDefaultedInt()).size shouldBe
            engine.serialize(ContainsDefaultedInt.serializer(), ContainsDefaultedInt(listOf(1, 2))).size
    }

    @Serializable
    data class ContainsDefaultedInt(
        @Serializable(with = List_0::class)
        val list: List<Int> = emptyList()
    ) {
        object List_0 : FixedLengthListSerializer<Int>(10, List_1)
        object List_1 : SerializerWithDefault<Int>(IntSerializer, 0)
    }
}
