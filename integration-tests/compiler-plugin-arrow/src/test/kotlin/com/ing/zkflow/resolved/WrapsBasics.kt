package com.ing.zkflow.resolved

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.WrappedKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
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

    @Serializable(with = Int_0::class)
    val int: @Contextual Int = 0,
) {
    @Serializable(with = String_0::class)
    val string: @Contextual String = "䶖万"

    object Char_0 : WrappedKSerializerWithDefault<Char>(ASCIICharSerializer)
    object Int_0 : WrappedKSerializerWithDefault<Int>(IntSerializer)
    object String_0 : FixedLengthUTF8StringSerializer(5)
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
