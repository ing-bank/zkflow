package com.ing.zkflow.serialization

import com.ing.zkflow.Default
import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthSetSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ComplexSetTest : SerializerTest {
    // Setup
    @ZKP
    data class ComplexSet(
        val set: @Size(5) Set<@Default<Baz>(Baz.Default::class) Baz> = setOf(Baz(0))
    )

    @ZKP
    data class Baz(
        val id: Int
    ) {
        object Default : DefaultProvider<Baz> {
            override val default: Baz = Baz(0)
        }
    }

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class ComplexSetResolved(
        @Serializable(with = Set_0::class) val set: @Contextual Set<@Contextual Baz> = setOf(Baz(0))
    ) {
        object Set_0 : FixedLengthSetSerializer<Baz>(5, Set_1)
        object Set_1 : SerializerWithDefault<Baz>(Set_2, Baz.Default.default)
        object Set_2 : WrappedFixedLengthKSerializer<Baz>(ComplexSetTest_Baz_Serializer, Baz::class.java.isEnum)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `ComplexSet makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(ComplexSetTest_ComplexSet_Serializer, ComplexSet())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ComplexSet generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            ComplexSetResolved.serializer(),
            ComplexSetResolved()
        ) shouldBe
            engine.serialize(ComplexSetTest_ComplexSet_Serializer, ComplexSet())
    }
}
