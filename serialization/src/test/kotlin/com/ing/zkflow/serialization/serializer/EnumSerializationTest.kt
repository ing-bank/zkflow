package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class EnumSerializationTest : SerializerTest {
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
        object Enum_0 : WrappedFixedLengthKSerializer<Option>(Option.serializer(), Option::class.java.isEnum)

        object Enums_0 : FixedLengthListSerializer<Option?>(5, Enums_1)
        object Enums_1 : NullableSerializer<Option>(Enums_2)
        object Enums_2 : SerializerWithDefault<Option>(Enums_3, Option.SECOND)
        object Enums_3 : WrappedFixedLengthKSerializer<Option>(Option.serializer(), Option::class.java.isEnum)
    }
}
