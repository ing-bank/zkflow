package com.ing.zkflow.resolved

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer.BigDecimalSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@Suppress("ClassName")
@Serializable
data class WrapsBigDecimal(
    @Serializable(with = BigDecimal_0::class)
    val bigDecimal: BigDecimal = 12.34.toBigDecimal()
) {
    object BigDecimal_0 : BigDecimalSerializer(5, 5)
}

class WrapsBigDecimalTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsBigDecimal make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsBigDecimal.serializer(),
            com.ing.zkflow.annotated.WrapsBigDecimal(), true
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
