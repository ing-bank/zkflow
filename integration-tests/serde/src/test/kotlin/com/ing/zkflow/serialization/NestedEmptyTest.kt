package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.generated.Empty0Serializer
import com.ing.zkflow.serialization.generated.NestedEmptySerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class NestedEmptyTest : SerializerTest {
    @ZKP
    data class NestedEmpty(
        val nested: Empty0 = Empty0()
    )

    // Setup
    // TODO naming is related to the problem outlined in
    //  fun KSClassDeclaration.getSurrogateClassName()
    @ZKP
    class Empty0 {
        override fun hashCode(): Int = 1

        override fun equals(other: Any?): Boolean = when (other) {
            is Empty0 -> true
            else -> false
        }
    }

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class NestedEmptyResolved(
        @Serializable(with = Nested_0::class)
        val nested: Empty0 = Empty0()
    ) {
        object Nested_0 : WrappedFixedLengthKSerializer<Empty0>(
            Empty0Serializer,
            Empty0::class.java.isEnum
        )
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `NestedEmpty makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(NestedEmptySerializer, NestedEmpty())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `NestedEmpty generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            NestedEmptyResolved.serializer(),
            NestedEmptyResolved()
        ) shouldBe
            engine.serialize(NestedEmptySerializer, NestedEmpty())
    }
}
