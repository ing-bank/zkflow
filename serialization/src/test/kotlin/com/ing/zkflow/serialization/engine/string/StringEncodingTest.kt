package com.ing.zkflow.serialization.engine.string

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUtf8StringSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class StringEncodingTest {
    @ParameterizedTest
    @MethodSource("data")
    fun `String intermediate representation must be correct`(scheme: BinaryFixedLengthScheme, string: String, expected: ByteArray) {
        scheme.encodeToBinary(FixedLengthUtf8StringSerializer(MAX_LENGTH), string) shouldBe expected
        scheme.decodeFromBinary(FixedLengthUtf8StringSerializer(MAX_LENGTH), expected) shouldBe string
    }

    companion object {
        const val MAX_LENGTH = 4

        @JvmStatic
        fun data() = listOf(
            Arguments.of(
                // UTF8
                ByteBinaryFixedLengthScheme, "a的",
                listOf(
                    listOf<Byte>(0, 0, 0, 4), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(97), // a
                    listOf<Byte>(-25, -102, -124), // 的
                    List(MAX_LENGTH - 4) { ByteSerializer.default } //
                ).flatten().toByteArray()
            ),
            Arguments.of(
                // ASCII
                ByteBinaryFixedLengthScheme, "ab",
                listOf(
                    listOf<Byte>(0, 0, 0, 2), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(97, 98),
                    List(MAX_LENGTH - 2) { ByteSerializer.default }
                ).flatten().toByteArray()
            ),
        )
    }
}
