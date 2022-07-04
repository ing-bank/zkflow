package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ManyNullablesTest : SerializerTest {
    // Setup
    @ZKP
    data class ManyNullables(
        val myList: @Size(5) List<@Size(5) List<@Size(5) List<Int?>?>?>? = null
    )

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class ManyNullablesResolved(
        @Serializable(with = MyList_0::class)
        val myList: @Contextual List<@Contextual List<@Contextual List<@Contextual Int?>?>?>? = null
    ) {
        object MyList_0 : NullableSerializer<List<List<List<Int?>?>?>>(MyList_1)
        object MyList_1 : FixedLengthListSerializer<List<List<Int?>?>?>(5, MyList_2)
        object MyList_2 : NullableSerializer<List<List<Int?>?>>(MyList_3)
        object MyList_3 : FixedLengthListSerializer<List<Int?>?>(5, MyList_4)
        object MyList_4 : NullableSerializer<List<Int?>>(MyList_5)
        object MyList_5 : FixedLengthListSerializer<Int?>(5, MyList_6)
        object MyList_6 : NullableSerializer<Int>(MyList_7)
        object MyList_7 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `ManyNullables makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(ManyNullablesTest_ManyNullables_Serializer, ManyNullables())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ManyNullables generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            ManyNullablesResolved.serializer(),
            ManyNullablesResolved()
        ) shouldBe
            engine.serialize(ManyNullablesTest_ManyNullables_Serializer, ManyNullables())
    }
}
