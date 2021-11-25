package com.ing.zkflow.resolved

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.WrappedKSerializerWithDefault
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@kotlinx.serialization.Serializable
data class WrapsManyNullables(
    @kotlinx.serialization.Serializable(with = MyList_0::class)
    val myList: @kotlinx.serialization.Contextual List<
        @kotlinx.serialization.Contextual List<
            @kotlinx.serialization.Contextual List<
                @kotlinx.serialization.Contextual Int?
                >?
            >?
        >? = null
) {
    object MyList_0 : NullableSerializer<List<List<List<Int?>?>?>>(MyList_1)
    object MyList_1 : FixedLengthListSerializer<List<List<Int?>?>?>(5, MyList_2)
    object MyList_2 : NullableSerializer<List<List<Int?>?>>(MyList_3)
    object MyList_3 : FixedLengthListSerializer<List<Int?>?>(5, MyList_4)
    object MyList_4 : NullableSerializer<List<Int?>>(MyList_5)
    object MyList_5 : FixedLengthListSerializer<Int?>(5, MyList_6)
    object MyList_6 : NullableSerializer<Int>(MyList_7)
    object MyList_7 : WrappedKSerializerWithDefault<Int>(IntSerializer)
}

class WrapsManyNullablesTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsManyNullables make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsManyNullables.serializer(),
            com.ing.zkflow.annotated.WrapsManyNullables()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsManyNullables generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsManyNullables.serializer(),
            com.ing.zkflow.annotated.WrapsManyNullables()
        ) shouldBe
            engine.serialize(WrapsManyNullables.serializer(), WrapsManyNullables())
    }
}
