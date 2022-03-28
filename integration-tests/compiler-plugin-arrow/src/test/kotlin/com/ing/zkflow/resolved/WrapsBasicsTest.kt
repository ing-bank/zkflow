package com.ing.zkflow.resolved

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.char.UTF8CharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthStringSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@Serializable
data class WrapsBasics(
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
    object Char_1 : WrappedFixedLengthKSerializerWithDefault<Char>(UTF8CharSerializer)
    object Int_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
    object String_0 : FixedLengthStringSerializer(6)
}

class WrapsBasicsTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsBasic make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsBasics.serializer(),
            com.ing.zkflow.annotated.WrapsBasics()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsBasic generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsBasics.serializer(),
            com.ing.zkflow.annotated.WrapsBasics()
        ) shouldBe
            engine.serialize(WrapsBasics.serializer(), WrapsBasics())
    }
}
