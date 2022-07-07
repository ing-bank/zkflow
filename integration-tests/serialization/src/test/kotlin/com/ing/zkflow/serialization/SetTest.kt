package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthSetSerializer
import com.ing.zkflow.serialization.serializer.string.FixedSizeUtf8StringSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SetTest : SerializerTest {
    // Setup
    @ZKP
    data class MySet(
        val set: @Size(5) Set<@UTF8(10) String> = setOf("test")
    )

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class MySetResolved(
        @Serializable(with = Set_0::class) val set: @Contextual Set<@Contextual String> = setOf("test")
    ) {
        object Set_0 : FixedLengthSetSerializer<String>(5, Set_1)
        object Set_1 : FixedSizeUtf8StringSerializer(10)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `MySet makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(SetTestMySetSerializer, MySet())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `MySet generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            MySetResolved.serializer(),
            MySetResolved()
        ) shouldBe
            engine.serialize(SetTestMySetSerializer, MySet())
    }
}
