package com.ing.zkflow.serialization

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Default
import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.generated.OutOfReachStarSurrogateSerializer
import com.ing.zkflow.serialization.generated.ThirdPartyWithStarTypeSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ThirdPartyWithStarTypeTest : SerializerTest {
    // Setup
    @ZKP
    data class ThirdPartyWithStarType(
        val myList: @Size(2) List<@Size(2) List<@Default<OutOfReachStar>(DefaultOutOfReachStar::class) @Via<OutOfReachStarSurrogate> OutOfReachStar?>?>? = null
    )

    object DefaultOutOfReachStar : DefaultProvider<OutOfReachStar> {
        override val default = OutOfReachStar(setOf(1))
    }

    object ConverterOutOfReachStar : ConversionProvider<OutOfReachStar, OutOfReachStarSurrogate> {
        override fun from(original: OutOfReachStar) = OutOfReachStarSurrogate(
            original.value.map {
                it as? Int ?: error("Only Ints are accepted")
            }.toSet()
        )
    }

    @ZKPSurrogate(ConverterOutOfReachStar::class)
    class OutOfReachStarSurrogate(val value: @Size(2) Set<Int>) : Surrogate<OutOfReachStar> {
        override fun toOriginal() = OutOfReachStar(value)
    }

    data class OutOfReachStar(val value: Set<*>)

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class ThirdPartyWithStarTypeResolved(
        @Serializable(with = MyList_0::class)
        val myList: @Contextual List<@Contextual List<@Contextual OutOfReachStar?>?>? = null
    ) {
        object MyList_0 : NullableSerializer<List<List<OutOfReachStar?>?>>(MyList_1)
        object MyList_1 : FixedLengthListSerializer<List<OutOfReachStar?>?>(2, MyList_2)
        object MyList_2 : NullableSerializer<List<OutOfReachStar?>>(MyList_3)
        object MyList_3 : FixedLengthListSerializer<OutOfReachStar?>(2, MyList_4)
        object MyList_4 : NullableSerializer<OutOfReachStar>(MyList_5)
        object MyList_5 : SerializerWithDefault<OutOfReachStar>(MyList_6, DefaultOutOfReachStar.default)
        object MyList_6 : WrappedFixedLengthKSerializer<OutOfReachStar>(
            OutOfReachStarSurrogateSerializer,
            OutOfReachStar::class.java.isEnum
        )
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `ThirdPartyWithStarType makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(ThirdPartyWithStarTypeSerializer, ThirdPartyWithStarType())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ThirdPartyWithStarType generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            ThirdPartyWithStarTypeResolved.serializer(),
            ThirdPartyWithStarTypeResolved()
        ) shouldBe
            engine.serialize(ThirdPartyWithStarTypeSerializer, ThirdPartyWithStarType())
    }
}
