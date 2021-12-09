package com.ing.zkflow.serialization.engine

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.BitBinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.ByteSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ByteEncodingTest {
    @ParameterizedTest
    @MethodSource("data")
    fun `Byte intermediate representation must be correct`(test: Triple<BinaryFixedLengthScheme, Byte, ByteArray>) {
        val (scheme, byte, expected) = test
        scheme.encodeToBinary(ByteSerializer, byte) shouldBe expected

        scheme.decodeFromBinary(ByteSerializer, expected) shouldBe byte
    }

    companion object {
        @JvmStatic
        fun data() = listOf(
            Triple(ByteBinaryFixedLengthScheme, 16, byteArrayOf(16)),
            Triple(BitBinaryFixedLengthScheme, 16, byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0)),
            Triple(ByteBinaryFixedLengthScheme, -1, byteArrayOf(-1)),
            Triple(BitBinaryFixedLengthScheme, -1, ByteArray(8) { 1 })
        )
    }
}
