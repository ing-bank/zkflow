package com.ing.zkflow.resolved

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@kotlinx.serialization.Serializable
data class WrapsSet(
    @kotlinx.serialization.Serializable(with = Set_0::class) val set:
        @kotlinx.serialization.Contextual Set<
            @kotlinx.serialization.Contextual String> = setOf("test")
) {
    object Set_0 : com.ing.zkflow.serialization.serializer.FixedLengthSetSerializer<String>(5, Set_1)
    object Set_1 : com.ing.zkflow.serialization.serializer.string.FixedLengthStringSerializer(10)
}

class WrapsSetTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsSet make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsSet.serializer(),
            com.ing.zkflow.annotated.WrapsSet()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsSet generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsSet.serializer(),
            com.ing.zkflow.annotated.WrapsSet()
        ) shouldBe
            engine.serialize(WrapsSet.serializer(), WrapsSet())
    }
}
