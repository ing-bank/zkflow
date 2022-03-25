package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.DefaultOutOfReach
import com.ing.zkflow.annotated.OutOfReach
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@kotlinx.serialization.Serializable
data class Wraps3rdPartyNullable(
    @kotlinx.serialization.Serializable(with = MyList_0::class)
    val myList: @kotlinx.serialization.Contextual List<
        @kotlinx.serialization.Contextual List<
            @kotlinx.serialization.Contextual OutOfReach?
            >?
        >? = null
) {
    object MyList_0 : NullableSerializer<List<List<OutOfReach?>?>>(MyList_1)
    object MyList_1 : FixedLengthListSerializer<List<OutOfReach?>?>(2, MyList_2)
    object MyList_2 : NullableSerializer<List<OutOfReach?>>(MyList_3)
    object MyList_3 : FixedLengthListSerializer<OutOfReach?>(2, MyList_4)
    object MyList_4 : NullableSerializer<OutOfReach>(MyList_5)
    object MyList_5 : SerializerWithDefault<OutOfReach>(MyList_6, DefaultOutOfReach.default)
    object MyList_6 : com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer<com.ing.zkflow.annotated.OutOfReach>(
        com.ing.zkflow.serialization.infra.OutOfReachSurrogateSurrogateSerializer,
        com.ing.zkflow.annotated.OutOfReach::class.java.isEnum
    )
}

// There will also be a resolved version for OutOfReachSurrogate as below.
// This version is commented out because, in compiler's eyes, this will be a different class,
// while we want to use the original one. This version is for the sake of completeness.
//
// @kotlinx.serialization.Serializable
// class OutOfReachSurrogate(
//     @kotlinx.serialization.Serializable(with = Value_0::class)
//     val value:@kotlinx.serialization.Contextual Int
// ): Surrogate<OutOfReach> {
//     override fun toOriginal(): OutOfReach = OutOfReach(value)
//
//     object Value_0: serializer.WrappedZSerializer<Int>(serializer.IntSerializer)
// }

class Wraps3rdPartyNullableTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `Wraps3rdPartyNullable make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.Wraps3rdPartyNullable.serializer(),
            com.ing.zkflow.annotated.Wraps3rdPartyNullable()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Wraps3rdPartyNullable generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.Wraps3rdPartyNullable.serializer(),
            com.ing.zkflow.annotated.Wraps3rdPartyNullable()
        ) shouldBe
            engine.serialize(Wraps3rdPartyNullable.serializer(), Wraps3rdPartyNullable())
    }
}
