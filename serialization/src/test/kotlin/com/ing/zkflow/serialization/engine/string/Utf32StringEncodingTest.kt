package com.ing.zkflow.serialization.engine.string

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUtf32StringSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class Utf32StringEncodingTest {
    private val serializer = FixedLengthUtf32StringSerializer(MAX_LENGTH)

    @ParameterizedTest
    @MethodSource("data")
    fun `String intermediate representation must be correct`(scheme: BinaryFixedLengthScheme, string: String, expected: ByteArray) {
        scheme.encodeToBinary(serializer, string) shouldBe expected
        scheme.decodeFromBinary(serializer, expected) shouldBe string
    }

    companion object {
        const val MAX_LENGTH = 8

        @JvmStatic
        fun data() = listOf(
            Arguments.of(
                // UTF8
                ByteBinaryFixedLengthScheme, "a的",
                listOf(
                    listOf<Byte>(0, 0, 0, 8), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(0, 0, 0, 97), // a
                    listOf<Byte>(0, 0, 118, -124), // 的
                    List(MAX_LENGTH - 8) { ByteSerializer.default } //
                ).flatten().toByteArray()
            ),
            Arguments.of(
                // ASCII
                ByteBinaryFixedLengthScheme, "ab",
                listOf(
                    listOf<Byte>(0, 0, 0, 8), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(0, 0, 0, 97, 0, 0, 0, 98),
                    List(MAX_LENGTH - 8) { ByteSerializer.default }
                ).flatten().toByteArray()
            ),
        )
    }
}
