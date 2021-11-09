package com.ing.zkflow.serialization.engine

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.BitBinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.IntSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class IntEncodingTest {
    @ParameterizedTest
    @MethodSource("data")
    fun `Int intermediate representation must be correct`(test: Triple<BinaryFixedLengthScheme, Int, ByteArray>) {
        val (scheme, int, expected) = test
        scheme.encodeToBinary(IntSerializer, int) shouldBe expected

        scheme.decodeFromBinary(IntSerializer, expected) shouldBe int
    }

    companion object {
        @JvmStatic
        fun data() = listOf(
            Triple(ByteBinaryFixedLengthScheme, 1024, byteArrayOf(0, 0, 4, 0)),
            Triple(
                BitBinaryFixedLengthScheme, 1024,
                listOf(List<Byte>(8) { 0 }, List<Byte>(8) { 0 }, listOf<Byte>(0, 0, 0, 0, 0, 1, 0, 0), List<Byte>(8) { 0 })
                    .flatten().toByteArray()
            )
        )
    }
}
