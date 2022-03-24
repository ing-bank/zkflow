package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.Option
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@Serializable
data class WrapsEnum(
    @Serializable(with = Option_0::class)
    val option: @Contextual Option = Option.FIRST
) {
    object Option_0 : WrappedFixedLengthKSerializer<Option>(Option.serializer(), Option::class.java.isEnum)
}

// There will also be a resolved version for Option as below.
// This version is commented out because, in compiler's eyes, this will be a different class,
// while we want to use the original one. This version is for the sake of completeness.
//
// @kotlinx.serialization.Serializable
// enum class Option {
//     FIRST,
//     SECOND
// }

class WrapsEnumTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsEnum make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsEnum.serializer(),
            com.ing.zkflow.annotated.WrapsEnum()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsEnum generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsEnum.serializer(),
            com.ing.zkflow.annotated.WrapsEnum()
        ) shouldBe
            engine.serialize(WrapsEnum.serializer(), WrapsEnum())
    }
}
