package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.BFLEngine
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer.BigDecimalSerializer
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
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

    @ParameterizedTest
    @MethodSource("engines")
    fun `Sizes for BigDecimals must match`(engine: BFLEngine) {
        // integer, fractional parts are serialized as bytes arrays with max size PRECISION,
        // additionally, `kind` and `sign` of the big integer are serialized.
        val expectedBytesSize = Byte.SIZE_BYTES + Byte.SIZE_BYTES + 2 * (IntSerializer.descriptor.byteSize + PRECISION * Byte.SIZE_BYTES)

        InstanceSerializer.descriptor.byteSize shouldBe expectedBytesSize
        engine.serialize(InstanceSerializer, BigDecimal.ONE).size shouldBe expectedBytesSize * engine.bytesScaler

        ContainsBigDecimal.serializer().descriptor.toFixedLengthSerialDescriptorOrThrow().byteSize shouldBe expectedBytesSize
        engine.serialize(ContainsBigDecimal.serializer(), ContainsBigDecimal()).size shouldBe expectedBytesSize * engine.bytesScaler
    }

    @Serializable
    data class ContainsBigDecimal(
        @Serializable(with = InstanceSerializer::class)
        val bigDecimal: BigDecimal = BigDecimal.TEN
    )

    companion object {
        const val PRECISION = 10
    }

    object InstanceSerializer : BigDecimalSerializer(PRECISION, PRECISION)
}
