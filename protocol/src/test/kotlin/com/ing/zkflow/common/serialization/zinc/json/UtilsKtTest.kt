package com.ing.zkflow.common.serialization.zinc.json

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class UtilsKtTest {
    @ParameterizedTest
    @CsvSource(
        "0,00000000",
        "1,00000001",
        "127,01111111",
        "-1,11111111",
        "-128,10000000"
    )
    fun toUnsignedBitString(byte: Byte, bitString: String) {
        ByteArray(1) { byte }.toUnsignedBitString() shouldBe bitString.toListOfJsonElement()
    }

    companion object {
        private fun String.toListOfJsonElement(): List<JsonElement> {
            assert(this.length == 8)
            return (0 until Byte.SIZE_BITS).map { i ->
                when (val c = this[i]) {
                    '0' -> JsonPrimitive(false)
                    '1' -> JsonPrimitive(true)
                    else -> throw IllegalArgumentException("$c not in [0, 1]")
                }
            }
        }
    }
}
