package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.ASCIIChar
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.UnicodeChar
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.generated.BasicsSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.char.UnicodeCharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedSizeUtf8StringSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BasicsTest : SerializerTest {
    // Setup
    @ZKP
    data class Basics(
        val char: @ASCIIChar Char = 'z',
        val unicodeChar: @UnicodeChar Char = '的',
        val int: Int = 0,
        val string: @UTF8(6) String = "䶖万"
    )

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class BasicsResolved(
        @Serializable(with = Char_0::class)
        val char: @Contextual Char = 'z',

        @Serializable(with = Char_1::class)
        val utf8Char: @Contextual Char = '的',

        @Serializable(with = Int_0::class)
        val int: @Contextual Int = 0,

        @Serializable(with = String_0::class)
        val string: @Contextual String = "䶖万"
    ) {
        object Char_0 : WrappedFixedLengthKSerializerWithDefault<Char>(ASCIICharSerializer)
        object Char_1 : WrappedFixedLengthKSerializerWithDefault<Char>(UnicodeCharSerializer)
        object Int_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
        object String_0 : FixedSizeUtf8StringSerializer(6)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `Basics makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(BasicsSerializer, Basics())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Basics generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            BasicsResolved.serializer(),
            BasicsResolved()
        ) shouldBe
            engine.serialize(BasicsSerializer, Basics())
    }
}
