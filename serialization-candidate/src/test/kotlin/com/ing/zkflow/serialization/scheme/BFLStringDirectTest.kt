package com.ing.zkflow.serialization.scheme

import com.ing.zkflow.engine.BFLEngine
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BFLStringDirectTest {
    private val engine = BFLEngine.Bytes

    private val string = "a的"
    private val direct = (
        listOf<Byte>(0, 0, 0, 2) + // number of characters
            listOf(listOf<Byte>(0, 97), listOf<Byte>(118, -124), listOf<Byte>(0, 45)).flatten() // string encoding per char, including the last filler char "-"
        ).toByteArray()

    @Test
    fun `String direct encoding and serialization must coincide`() {
        val ser = engine.serialize(FixedLengthUTF8StringSerializer(3), string)
        ser shouldBe direct
    }

    @Test
    fun `String and deserialization must coincide`() {
        engine.deserialize(FixedLengthUTF8StringSerializer(3), direct) shouldBe string
    }
}
