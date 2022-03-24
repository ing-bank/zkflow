package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.DefaultFlag
import com.ing.zkflow.annotated.Flag
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@kotlinx.serialization.Serializable
data class WrapsOwnNullable(
    @kotlinx.serialization.Serializable(with = MyList_0::class)
    val myList: @kotlinx.serialization.Contextual List<
        @kotlinx.serialization.Contextual List<
            @kotlinx.serialization.Contextual Flag?
            >?
        >? = null
) {
    object MyList_0 : NullableSerializer<List<List<Flag?>?>>(MyList_1)
    object MyList_1 : FixedLengthListSerializer<List<Flag?>?>(2, MyList_2)
    object MyList_2 : NullableSerializer<List<Flag?>>(MyList_3)
    object MyList_3 : FixedLengthListSerializer<Flag?>(2, MyList_4)
    object MyList_4 : NullableSerializer<Flag>(MyList_5)
    object MyList_5 : SerializerWithDefault<Flag>(MyList_6, DefaultFlag.default)
    object MyList_6 : WrappedFixedLengthKSerializer<Flag>(Flag.serializer(), Flag::class.java.isEnum)
}

// There will also be a resolved version for Flag as below.
// This version is commented out because, in compiler's eyes, this will be a different class,
// while we want to use the original one. This version is for the sake of completeness.
//
// @kotlinx.serialization.Serializable
// data class Flag(@kotlinx.serialization.Serializable(with = Value_0::class) val value: Int) {
//     object Value_0: serializer.WrappedZSerializer<Int>(serializer.IntSerializer)
// }

class WrapsOwnNullableTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsOwnNullable make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsOwnNullable.serializer(),
            com.ing.zkflow.annotated.WrapsOwnNullable()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsOwnNullable generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsOwnNullable.serializer(),
            com.ing.zkflow.annotated.WrapsOwnNullable()
        ) shouldBe
            engine.serialize(WrapsOwnNullable.serializer(), WrapsOwnNullable())
    }
}
