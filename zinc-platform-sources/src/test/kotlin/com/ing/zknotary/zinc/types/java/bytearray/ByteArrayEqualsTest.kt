package com.ing.zknotary.zinc.types.java.bytearray

import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ByteArrayEqualsTest {
    private val zincZKService = getZincZKService<ByteArrayEqualsTest>()

    @Test
    fun `identity test`() {
        val bytes = "Hello World!".toByteArray()
        performEqualityTest(bytes, bytes, true)
    }

    @Test
    fun `ByteArrays of different sizes should not be equal`() {
        val bytes = "Hello World!".toByteArray()
        val otherBytes = "Hello World! Have a great day!".toByteArray()
        performEqualityTest(bytes, otherBytes, false)
    }

    @Test
    fun `different ByteArrays of equal sizes should not be equal`() {
        val bytes = "Hello World!".toByteArray()
        val otherBytes = "Hallo World!".toByteArray()
        performEqualityTest(bytes, otherBytes, false)
    }

    private fun performEqualityTest(
        left: ByteArray,
        right: ByteArray,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject(32))
            put("right", right.toJsonObject(32))
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
