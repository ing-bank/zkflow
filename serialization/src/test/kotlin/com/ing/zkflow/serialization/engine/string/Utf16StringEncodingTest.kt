package com.ing.zkflow.serialization.engine.string

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.string.FixedSizeUtf16StringSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class Utf16StringEncodingTest {
    private val serializer = FixedSizeUtf16StringSerializer(MAX_LENGTH)

    @ParameterizedTest
    @MethodSource("data")
    fun `String intermediate representation must be correct`(scheme: BinaryFixedLengthScheme, string: String, expected: ByteArray) {
        scheme.encodeToBinary(serializer, string) shouldBe expected
        scheme.decodeFromBinary(serializer, expected) shouldBe string
    }

    companion object {
        const val MAX_LENGTH = 6

        @JvmStatic
        fun data() = listOf(
            Arguments.of(
                // UTF8
                ByteBinaryFixedLengthScheme, "a的",
                listOf(
                    listOf<Byte>(0, 0, 0, 6), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(-2, -1), // UTF16 marker?
                    listOf<Byte>(0, 97), // a
                    listOf<Byte>(118, -124), // 的
                    List(MAX_LENGTH - 6) { ByteSerializer.default } //
                ).flatten().toByteArray()
            ),
            Arguments.of(
                // ASCII
                ByteBinaryFixedLengthScheme, "ab",
                listOf(
                    listOf<Byte>(0, 0, 0, 6), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(-2, -1, 0, 97, 0, 98),
                    List(MAX_LENGTH - 6) { ByteSerializer.default }
                ).flatten().toByteArray()
            ),
        )
    }
}
