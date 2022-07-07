package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class EmptyTest : SerializerTest {
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
    @Serializable
    class EmptyResolved {
        override fun hashCode(): Int = 1

        override fun equals(other: Any?): Boolean = when (other) {
            is EmptyResolved -> true
            else -> false
        }
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `Empty makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(EmptyTestEmptySerializer, Empty())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Empty generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            EmptyResolved.serializer(),
            EmptyResolved()
        ) shouldBe
            engine.serialize(EmptyTestEmptySerializer, Empty())
    }
}
