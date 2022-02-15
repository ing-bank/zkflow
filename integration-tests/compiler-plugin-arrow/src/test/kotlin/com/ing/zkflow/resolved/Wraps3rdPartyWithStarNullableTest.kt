package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.ConverterOutOfReachStar
import com.ing.zkflow.annotated.DefaultOutOfReachStar
import com.ing.zkflow.annotated.OutOfReachStar
import com.ing.zkflow.annotated.OutOfReachStarSurrogate
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@kotlinx.serialization.Serializable
data class Wraps3rdPartyWithStar(
    @kotlinx.serialization.Serializable(with = MyList_0::class)
    val myList: @kotlinx.serialization.Contextual List<
        @kotlinx.serialization.Contextual List<
            @kotlinx.serialization.Contextual OutOfReachStar?
            >?
        >? = null
) {
    object MyList_0 : NullableSerializer<List<List<OutOfReachStar?>?>>(MyList_1)
    object MyList_1 : FixedLengthListSerializer<List<OutOfReachStar?>?>(2, MyList_2)
    object MyList_2 : NullableSerializer<List<OutOfReachStar?>>(MyList_3)
    object MyList_3 : FixedLengthListSerializer<OutOfReachStar?>(2, MyList_4)
    object MyList_4 : NullableSerializer<OutOfReachStar>(MyList_5)
    object MyList_5 : SerializerWithDefault<OutOfReachStar>(MyList_6, DefaultOutOfReachStar.default)
    object MyList_6 : SurrogateSerializer<OutOfReachStar, OutOfReachStarSurrogate>(
        OutOfReachStarSurrogate.serializer(), { ConverterOutOfReachStar.from(it) }
    )
}

// There will also be a resolved version for OutOfReachSurrogate as below.      
// This version is commented out because, in compiler's eyes, this will be a different class,
// while we want to use the original one. This version is for the sake of completeness.
//
// @kotlinx.serialization.Serializable
// class OutOfReachStarSurrogate(
//     @kotlinx.serialization.Serializable(with = Value_0::class)
//     val value:@kotlinx.serialization.Contextual Set<Int>
// ): Surrogate<OutOfReach> {
//     override fun toOriginal(): OutOfReachStar = OutOfReachStar(value)
//
//     object Value_0: com.ing.serialization.serializer.BoundedSetSerializer<Int>(2, Value_1)
//     object Value_1: com.ing.serialization.serializer.WrappedZSerializer<Int>(com.ing.serialization.serializer.IntSerializer)
// }

class Wraps3rdPartyWithStarTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `Wraps3rdPartyNullable make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.Wraps3rdPartyWithStar.serializer(),
            com.ing.zkflow.annotated.Wraps3rdPartyWithStar()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Wraps3rdPartyNullable generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.Wraps3rdPartyWithStar.serializer(),
            com.ing.zkflow.annotated.Wraps3rdPartyWithStar()
        ) shouldBe
            engine.serialize(Wraps3rdPartyWithStar.serializer(), Wraps3rdPartyWithStar())
    }
}
