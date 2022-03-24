package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.Baz
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@kotlinx.serialization.Serializable
data class WrapsSetComplex(
    @kotlinx.serialization.Serializable(with = Set_0::class) val set:
        @kotlinx.serialization.Contextual Set<
            @kotlinx.serialization.Contextual Baz> = setOf(Baz(0))
) {
    object Set_0 : com.ing.zkflow.serialization.serializer.FixedLengthSetSerializer<Baz>(5, Set_1)
    object Set_1 : com.ing.zkflow.serialization.serializer.SerializerWithDefault<Baz>(Set_2, Baz.Default.default)
    object Set_2 : com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer<Baz>(Baz.serializer(), Baz::class.java.isEnum)
}

// There will also be a resolved version for Baz as below.
// This version is commented out because, in compiler's eyes, this will be a different class,
// while we want to use the original one. This version is for the sake of completeness.
//
// @kotlinx.serialization.Serializable
// data class Baz(
//     @kotlinx.serialization.Serializable(with = Id_0::class) val id: @kotlinx.serialization.Contextual Int
// ) {
//     object Default : DefaultProvider<Baz> {
//         override val default: Baz = Baz(0)
//     }
//
//     object Id_0: com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault<Int>(com.ing.zkflow.serialization.serializer.IntSerializer)
// }

class WrapsSetComplexTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsSetComplex make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsSetComplex.serializer(),
            com.ing.zkflow.annotated.WrapsSetComplex()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsSetComplex generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsSetComplex.serializer(),
            com.ing.zkflow.annotated.WrapsSetComplex()
        ) shouldBe
            engine.serialize(WrapsSetComplex.serializer(), WrapsSetComplex())
    }
}
