package com.ing.zkflow.serialization.scheme

import com.ing.zkflow.serialization.serializer.DefaultedSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerTest
import com.ing.zkflow.serialization.serializer.WrappedKSerializer
import engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class EnumTest : SerializerTest {
    @Serializable
    enum class Option {
        FIRST,
        SECOND
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Enum must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(Option.serializer(), Option.FIRST)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Enum variants must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(Option.serializer(), Option.FIRST).size shouldBe
            engine.serialize(Option.serializer(), Option.SECOND).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List Enum must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsEnums.serializer(), ContainsEnums())
    }

    @Serializable
    data class ContainsEnums(
        @Serializable(with = Enum_0::class)
        val enum: Option = Option.FIRST,

        @Serializable(with = Enums_0::class)
        val enums: List<Option?> = listOf(Option.SECOND, null)
    ) {
        object Enum_0 : WrappedKSerializer<Option>(Option.serializer())

        object Enums_0 : FixedLengthListSerializer<Option?>(5, Enums_1)
        object Enums_1 : NullableSerializer<Option>(Enums_2)
        object Enums_2 : DefaultedSerializer<Option>(Enums_3, Option.SECOND)
        object Enums_3 : WrappedKSerializer<Option>(Option.serializer())
    }
}
