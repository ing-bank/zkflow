package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.string.FixedSizeUtf8StringSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MapTest : SerializerTest {
    // Setup
    @ZKP
    data class MyMap(
        val map: @Size(5) Map<Int, @UTF8(10) String?> = mapOf(1 to "test", 2 to null)
    )

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class MyMapResolved(
        @Serializable(with = Map_0::class)
        val map: Map<Int, String?> = mapOf(1 to "test", 2 to null)
    ) {
        object Map_0 : FixedLengthMapSerializer<Int, String?>(
            5, Map_0_A_0, Map_0_B_0
        )
        object Map_0_A_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
        object Map_0_B_0 : NullableSerializer<String>(Map_0_B_1)
        object Map_0_B_1 : FixedSizeUtf8StringSerializer(10)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `MyMap makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(MapTest_MyMap_Serializer, MyMap())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `MyMap generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            MyMapResolved.serializer(),
            MyMapResolved()
        ) shouldBe
            engine.serialize(MapTest_MyMap_Serializer, MyMap())
    }
}
