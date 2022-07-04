package com.ing.zkflow.serialization

import com.ing.zkflow.Default
import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import com.ing.zkflow.serialization.serializer.string.FixedSizeUtf8StringSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MapComplexTest : SerializerTest {
    // Setup
    @ZKP
    data class MapComplex(
        val map: @Size(11) Map<
            @UTF8(13) String,
            @Default<Baz>(Baz.Default::class) Baz
            > = mapOf("test" to Baz(1))
    )

    @ZKP
    data class Baz(val id: Int) {
        object Default : DefaultProvider<Baz> {
            override val default: Baz = Baz(0)
        }
    }

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class MapComplexResolved(
        @Serializable(with = Map_0::class) val map: @Contextual Map<@Contextual String, @Contextual Baz> = mapOf("test" to Baz(1))
    ) {

        object Map_0 : FixedLengthMapSerializer<String, Baz>(
            11, Map_0_A_0, Map_0_B_0
        )
        object Map_0_A_0 : FixedSizeUtf8StringSerializer(13)
        object Map_0_B_0 : SerializerWithDefault<Baz>(Map_0_B_1, Baz.Default.default)
        object Map_0_B_1 : WrappedFixedLengthKSerializer<Baz>(MapComplexTest_Baz_Serializer, Baz::class.java.isEnum)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `MapComplex makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(MapComplexTest_MapComplex_Serializer, MapComplex())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `MapComplex generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            MapComplexResolved.serializer(),
            MapComplexResolved()
        ) shouldBe
            engine.serialize(MapComplexTest_MapComplex_Serializer, MapComplex())
    }
}
