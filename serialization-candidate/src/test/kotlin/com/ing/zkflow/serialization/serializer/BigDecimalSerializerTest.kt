package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer.BigDecimalSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

class BigDecimalSerializerTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `BigDecimal must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, BigDecimal.TEN)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `BigDecimals must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, BigDecimal.ZERO).size shouldBe
            engine.serialize(InstanceSerializer, BigDecimal.ONE).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with BigDecimal must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsBigDecimal.serializer(), ContainsBigDecimal())
    }

    @Serializable
    data class ContainsBigDecimal(
        @Serializable(with = InstanceSerializer::class)
        val bigDecimal: BigDecimal = BigDecimal.TEN
    )

    object InstanceSerializer : BigDecimalSerializer(10, 10)
}
