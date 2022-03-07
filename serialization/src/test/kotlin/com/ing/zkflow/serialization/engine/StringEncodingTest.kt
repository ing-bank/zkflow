package com.ing.zkflow.serialization.engine

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.char.UTF8CharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer

class StringEncodingTest {
    @ParameterizedTest
    @MethodSource("data")
    fun `String intermediate representation must be correct`(test: Triple<BinaryFixedLengthScheme, String, ByteArray>) {
        val (scheme, string, expected) = test
        scheme.encodeToBinary(FixedLengthUTF8StringSerializer(3), string) shouldBe expected

        scheme.decodeFromBinary(FixedLengthUTF8StringSerializer(3), expected) shouldBe string
    }

    companion object {
        @JvmStatic
        fun data() = listOf(
            Triple(
                ByteBinaryFixedLengthScheme, "a的",
                listOf(
                    listOf<Byte>(0, 0, 0, 2), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(0, 97), // a
                    listOf<Byte>(118, -124), // 的
                    ByteBuffer
                        .allocate(Char.SIZE_BYTES)
                        .putChar(UTF8CharSerializer.default)
                        .array().toList() // filler char
                ).flatten().toByteArray()
            )
        )
    }
}
