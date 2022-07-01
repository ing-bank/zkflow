package com.ing.zkflow.serialization

import com.ing.zkflow.Default
import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.generated.Baz0Serializer
import com.ing.zkflow.serialization.generated.ComplexSetSerializer
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
        val set: @Size(5) Set<@Default<Baz0>(Baz0.Default::class) Baz0> = setOf(Baz0(0))
    )

    // TODO naming is related to the problem outlined in
    //  fun KSClassDeclaration.getSurrogateClassName()
    @ZKP
    data class Baz0(
        val id: Int
    ) {
        object Default : DefaultProvider<Baz0> {
            override val default: Baz0 = Baz0(0)
        }
    }

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class ComplexSetResolved(
        @Serializable(with = Set_0::class) val set: @Contextual Set<@Contextual Baz0> = setOf(Baz0(0))
    ) {
        object Set_0 : FixedLengthSetSerializer<Baz0>(5, Set_1)
        object Set_1 : SerializerWithDefault<Baz0>(Set_2, Baz0.Default.default)
        object Set_2 : WrappedFixedLengthKSerializer<Baz0>(Baz0Serializer, Baz0::class.java.isEnum)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `ComplexSet makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(ComplexSetSerializer, ComplexSet())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ComplexSet generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            ComplexSetResolved.serializer(),
            ComplexSetResolved()
        ) shouldBe
            engine.serialize(ComplexSetSerializer, ComplexSet())
    }
}
