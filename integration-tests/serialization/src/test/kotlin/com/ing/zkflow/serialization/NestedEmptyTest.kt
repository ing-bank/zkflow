package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class NestedEmptyTest : SerializerTest {
    @ZKP
    data class NestedEmpty(
        val nested: Empty = Empty()
    )

    // Setup
    @ZKP
    class Empty {
        override fun hashCode(): Int = 1

        override fun equals(other: Any?): Boolean = when (other) {
            is Empty -> true
            else -> false
        }
    }

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class NestedEmptyResolved(
        @Serializable(with = Nested_0::class)
        val nested: Empty = Empty()
    ) {
        object Nested_0 : WrappedFixedLengthKSerializer<Empty>(
            NestedEmptyTestEmptySerializer,
            Empty::class.java.isEnum
        )
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `NestedEmpty makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(NestedEmptyTestNestedEmptySerializer, NestedEmpty())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `NestedEmpty generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            NestedEmptyResolved.serializer(),
            NestedEmptyResolved()
        ) shouldBe
            engine.serialize(NestedEmptyTestNestedEmptySerializer, NestedEmpty())
    }
}
