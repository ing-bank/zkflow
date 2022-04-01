package com.ing.zkflow.serialization.engine.string

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.string.FixedSizeAsciiStringSerializer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AsciiStringEncodingTest {
    private val serializer = FixedSizeAsciiStringSerializer(MAX_LENGTH)

    @ParameterizedTest
    @MethodSource("data")
    fun `String intermediate representation must be correct`(scheme: BinaryFixedLengthScheme, string: String, expected: ByteArray) {
        scheme.encodeToBinary(serializer, string) shouldBe expected
        scheme.decodeFromBinary(serializer, expected) shouldBe string
    }

    @ParameterizedTest
    @MethodSource("nonAsciiData")
    fun `String with non-ascii characters cannot be encoded`(scheme: BinaryFixedLengthScheme, string: String) {
        shouldThrow<IllegalStateException> {
            scheme.encodeToBinary(serializer, string)
        }.message shouldBe "String `a的` cannot be encoded with ISO-8859-1"
    }

    companion object {
        const val MAX_LENGTH = 4

        @JvmStatic
        fun nonAsciiData() = listOf(
            Arguments.of(
                // UTF8
                ByteBinaryFixedLengthScheme,
                "a的",
            )
        )

        @JvmStatic
        fun data() = listOf(
            Arguments.of(
                // ASCII
                ByteBinaryFixedLengthScheme,
                "ab",
                listOf(
                    listOf<Byte>(0, 0, 0, 2), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(97, 98),
                    List(MAX_LENGTH - 2) { ByteSerializer.default }
                ).flatten().toByteArray(),
            ),
        )
    }
}
