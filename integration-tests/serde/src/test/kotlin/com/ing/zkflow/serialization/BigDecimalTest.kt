package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.generated.MyBigDecimalSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

class BigDecimalTest : SerializerTest {
    // Setup
    @ZKP
    data class MyBigDecimal(
        val bigDecimal: @BigDecimalSize(5, 5) BigDecimal = 12.34.toBigDecimal()
    )

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class BigDecimalResolved(
        @Serializable(with = BigDecimal_0::class)
        val bigDecimal: @Contextual BigDecimal = 12.34.toBigDecimal()
    ) {
        object BigDecimal_0 : FixedLengthFloatingPointSerializer.BigDecimalSerializer(5, 5)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `MyBigDecimal makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(MyBigDecimalSerializer, MyBigDecimal())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `MyBigDecimal generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            BigDecimalResolved.serializer(),
            BigDecimalResolved()
        ) shouldBe
            engine.serialize(MyBigDecimalSerializer, MyBigDecimal())
    }
}
