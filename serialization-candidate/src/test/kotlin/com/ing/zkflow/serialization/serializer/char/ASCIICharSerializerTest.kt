package com.ing.zkflow.serialization.serializer.char

import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerTest
import com.ing.zkflow.serialization.serializer.WrappedKSerializerWithDefault
import engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ASCIICharSerializerTest : SerializerTest {
    private val char = 'q'

    @ParameterizedTest
    @MethodSource("engines")
    fun `ASCII char must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ASCIICharSerializer, char)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ASCII chars must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(ASCIICharSerializer, char).size shouldBe
            engine.serialize(ASCIICharSerializer, 'z').size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List ASCII Char must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsChars.serializer(), ContainsChars())
    }

    @Serializable
    data class ContainsChars(
        @Serializable(with = ASCIICharSerializer::class)
        val char: Char = 'a',

        @Serializable(with = Char_0::class)
        val chars: List<Char?> = listOf('a', null)
    ) {
        object Char_0 : FixedLengthListSerializer<Char?>(5, Char_1)
        object Char_1 : NullableSerializer<Char>(Char_2)
        object Char_2 : WrappedKSerializerWithDefault<Char>(ASCIICharSerializer)
    }
}
