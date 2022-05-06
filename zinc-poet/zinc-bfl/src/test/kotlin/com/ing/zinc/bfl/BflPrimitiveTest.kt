package com.ing.zinc.bfl

import com.ing.zkflow.util.bitSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class BflPrimitiveTest {
    @ParameterizedTest
    @MethodSource("fixtures")
    fun `toStructureTree should get size and structure correct`(type: BflPrimitive) {
        val actual = type.toStructureTree()
        actual.bitSize shouldBe type.bitSize
        actual.toString() shouldBe """
            ${type.id}: ${type.bitSize} bits (${type.bitSize / 8} bytes)
        """.trimIndent()
    }

    companion object {
        @JvmStatic
        fun fixtures(): List<Arguments> = BflPrimitive.values()
            .map { Arguments.of(it) }
            .toList()
    }
}
