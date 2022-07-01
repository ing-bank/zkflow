package com.ing.zkflow.serialization

import com.ing.zkflow.Default
import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.generated.FlagSerializer
import com.ing.zkflow.serialization.generated.OwnNullableSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class OwnNullableTest : SerializerTest {
    // Setup
    @ZKP
    data class OwnNullable(
        val myList: @Size(2) List<@Size(2) List<@Default<Flag>(DefaultFlag::class) Flag?>?>? = null
    )

    @ZKP
    data class Flag(val value: Int)

    object DefaultFlag : DefaultProvider<Flag> {
        override val default = Flag(1)
    }

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class OwnNullableResolved(
        @Serializable(with = MyList_0::class)
        val myList: @Contextual List<@Contextual List<@Contextual Flag?>?>? = null
    ) {
        object MyList_0 : NullableSerializer<List<List<Flag?>?>>(MyList_1)
        object MyList_1 : FixedLengthListSerializer<List<Flag?>?>(2, MyList_2)
        object MyList_2 : NullableSerializer<List<Flag?>>(MyList_3)
        object MyList_3 : FixedLengthListSerializer<Flag?>(2, MyList_4)
        object MyList_4 : NullableSerializer<Flag>(MyList_5)
        object MyList_5 : SerializerWithDefault<Flag>(MyList_6, DefaultFlag.default)
        object MyList_6 : WrappedFixedLengthKSerializer<Flag>(FlagSerializer, Flag::class.java.isEnum)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `OwnNullable makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(OwnNullableSerializer, OwnNullable())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `OwnNullable generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            OwnNullableResolved.serializer(),
            OwnNullableResolved()
        ) shouldBe
            engine.serialize(OwnNullableSerializer, OwnNullable())
    }
}
