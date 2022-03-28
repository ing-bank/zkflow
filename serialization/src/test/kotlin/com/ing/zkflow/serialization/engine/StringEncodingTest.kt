package com.ing.zkflow.serialization.engine

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthStringSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class StringEncodingTest {
    @ParameterizedTest
    @MethodSource("data")
    fun `String intermediate representation must be correct`(test: Triple<BinaryFixedLengthScheme, String, ByteArray>) {
        val (scheme, string, expected) = test
        scheme.encodeToBinary(FixedLengthStringSerializer(MAX_LENGTH), string) shouldBe expected

        scheme.decodeFromBinary(FixedLengthStringSerializer(MAX_LENGTH), expected) shouldBe string
    }

    companion object {
        private const val MAX_LENGTH = 4

        @JvmStatic
        fun data() = listOf(
            Triple(
                ByteBinaryFixedLengthScheme, "a的",
                listOf(
                    listOf<Byte>(0, 0, 0, 4), // number of characters
                    // 3 character string including the filler char.
                    listOf<Byte>(97), // a
                    listOf<Byte>(-25, -102, -124), // 的
                    List(MAX_LENGTH - 4) { ByteSerializer.default }
                ).flatten().toByteArray()
            )
        )
    }
}
