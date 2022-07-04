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
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ThirdPartyNullableTest : SerializerTest {
    // Setup
    @ZKP
    data class ThirdPartyNullable(
        val myList: @Size(2) List<@Size(2) List<@Default<OutOfReach>(DefaultOutOfReach::class) @Via<OutOfReachSurrogate> OutOfReach?>?>? = null
    )

    object DefaultOutOfReach : DefaultProvider<OutOfReach> {
        override val default = OutOfReach(5)
    }

    object ConverterOutOfReach : ConversionProvider<OutOfReach, OutOfReachSurrogate> {
        override fun from(original: OutOfReach) = OutOfReachSurrogate(original.value)
    }

    @ZKPSurrogate(ConverterOutOfReach::class)
    class OutOfReachSurrogate(val value: Int) : Surrogate<OutOfReach> {
        override fun toOriginal() = OutOfReach(value)
    }

    data class OutOfReach(val value: Int)

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class ThirdPartyNullableResolved(
        @Serializable(with = MyList_0::class)
        val myList: @Contextual List<@Contextual List<@Contextual OutOfReach?>?>? = null
    ) {
        object MyList_0 : NullableSerializer<List<List<OutOfReach?>?>>(MyList_1)
        object MyList_1 : FixedLengthListSerializer<List<OutOfReach?>?>(2, MyList_2)
        object MyList_2 : NullableSerializer<List<OutOfReach?>>(MyList_3)
        object MyList_3 : FixedLengthListSerializer<OutOfReach?>(2, MyList_4)
        object MyList_4 : NullableSerializer<OutOfReach>(MyList_5)
        object MyList_5 : SerializerWithDefault<OutOfReach>(MyList_6, DefaultOutOfReach.default)
        object MyList_6 : WrappedFixedLengthKSerializer<OutOfReach>(
            ThirdPartyNullableTest_OutOfReachSurrogate_Serializer,
            OutOfReach::class.java.isEnum
        )
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `ThirdPartyNullable makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(ThirdPartyNullableTest_ThirdPartyNullable_Serializer, ThirdPartyNullable())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ThirdPartyNullable generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            ThirdPartyNullableResolved.serializer(),
            ThirdPartyNullableResolved()
        ) shouldBe
            engine.serialize(ThirdPartyNullableTest_ThirdPartyNullable_Serializer, ThirdPartyNullable())
    }
}
