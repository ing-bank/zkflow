package com.ing.zkflow.serialization.engine

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.BitBinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.ShortSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ShortEncodingTest {
    @ParameterizedTest
    @MethodSource("data")
    fun `Short intermediate representation must be correct`(test: Triple<BinaryFixedLengthScheme, Short, ByteArray>) {
        val (scheme, int, expected) = test
        scheme.encodeToBinary(ShortSerializer, int) shouldBe expected

        scheme.decodeFromBinary(ShortSerializer, expected) shouldBe int
    }

    companion object {
        @JvmStatic
        fun data() = listOf(
            Triple(ByteBinaryFixedLengthScheme, 1024, byteArrayOf(4, 0)),
            Triple(
                BitBinaryFixedLengthScheme, 1024,
                listOf(
                    listOf<Byte>(0, 0, 0, 0, 0, 1, 0, 0),
                    List<Byte>(8) { 0 }
                ).flatten().toByteArray()
            ),
            Triple(ByteBinaryFixedLengthScheme, -1, ByteArray(2) { 0xFF.toByte() }),
            Triple(BitBinaryFixedLengthScheme, -1, ByteArray(16) { 1 })
        )
    }
}
