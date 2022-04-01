package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.Baz
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@kotlinx.serialization.Serializable
data class WrapsMapComplex(
    @kotlinx.serialization.Serializable(with = Map_0::class) val map:
        @kotlinx.serialization.Contextual Map<
            @kotlinx.serialization.Contextual String,
            @kotlinx.serialization.Contextual Baz
            > = mapOf("test" to Baz(1))
) {

    object Map_0 : com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer<String, Baz>(
        11, Map_0_A_0, Map_0_B_0
    )
    object Map_0_A_0 : com.ing.zkflow.serialization.serializer.string.FixedSizeUtf8StringSerializer(13)
    object Map_0_B_0 : com.ing.zkflow.serialization.serializer.SerializerWithDefault<Baz>(Map_0_B_1, Baz.Default.default)
    object Map_0_B_1 : com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer<Baz>(Baz.serializer(), Baz::class.java.isEnum)
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

class WrapsMapComplexTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsMapComplex make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsMapComplex.serializer(),
            com.ing.zkflow.annotated.WrapsMapComplex()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsMapComplex generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsMapComplex.serializer(),
            com.ing.zkflow.annotated.WrapsMapComplex()
        ) shouldBe
            engine.serialize(WrapsMapComplex.serializer(), WrapsMapComplex())
    }
}
