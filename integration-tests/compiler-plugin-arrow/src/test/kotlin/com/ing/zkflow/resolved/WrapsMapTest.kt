package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.WrapsMap
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@kotlinx.serialization.Serializable
data class WrapsMap(
    @kotlinx.serialization.Serializable(with = Map_0::class)
    val map: Map<Int, String?> = mapOf(1 to "test", 2 to null)
) {
    object Map_0 : com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer<Int, String?>(
        5, Map_0_A_0, Map_0_B_0
    )
    object Map_0_A_0 : com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault<Int>(com.ing.zkflow.serialization.serializer.IntSerializer)
    object Map_0_B_0 : com.ing.zkflow.serialization.serializer.NullableSerializer<String>(Map_0_B_1)
    object Map_0_B_1 : com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer(10)
}

class WrapsMapTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsBigDecimal make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            WrapsMap.serializer(),
            WrapsMap(), true
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsBigDecimal generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsBigDecimal.serializer(),
            com.ing.zkflow.annotated.WrapsBigDecimal()
        ) shouldBe
            engine.serialize(WrapsBigDecimal.serializer(), WrapsBigDecimal())
    }
}
