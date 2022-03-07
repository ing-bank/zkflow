package com.ing.zkflow.serialization.engine.string

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ASCIIStringEncodingTest {
    @ParameterizedTest
    @MethodSource("data")
    fun `String intermediate representation must be correct`(test: Triple<BinaryFixedLengthScheme, String, ByteArray>) {
        val (scheme, string, expected) = test
        scheme.encodeToBinary(FixedLengthASCIIStringSerializer(3), string) shouldBe expected

        scheme.decodeFromBinary(FixedLengthASCIIStringSerializer(3), expected) shouldBe string
    }

    companion object {
        @JvmStatic
        fun data() = listOf(
            Triple(
                // ASCII
                ByteBinaryFixedLengthScheme, "ab",
                listOf(
                    listOf<Byte>(0, 0, 0, 2), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(97, 98, ASCIICharSerializer.default.toInt().toByte())
                ).flatten().toByteArray()
            )
        )
    }
}
