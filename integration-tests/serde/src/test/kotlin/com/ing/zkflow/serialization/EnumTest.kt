package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.generated.EnumSerializer
import com.ing.zkflow.serialization.generated.OptionSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class EnumTest : SerializerTest {
    // Setup
    @ZKP
    data class Enum(val option: Option = Option.FIRST)

    @ZKP
    enum class Option {
        FIRST,
        SECOND
    }

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class EnumResolved(
        @Serializable(with = Option_0::class)
        val option: @Contextual Option = Option.FIRST
    ) {
        object Option_0 : WrappedFixedLengthKSerializer<Option>(OptionSerializer, Option::class.java.isEnum)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `Enum makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(EnumSerializer, Enum())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Enum generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            EnumResolved.serializer(),
            EnumResolved()
        ) shouldBe
            engine.serialize(EnumSerializer, Enum())
    }
}
