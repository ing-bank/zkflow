package com.ing.zinc.bfl

import com.ing.zkflow.util.bitSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BflArrayTest {
    @Test
    fun `toStructureTree should get size and structure correctly`() {
        val testSubject = BflArray(3, BflPrimitive.U8)
        val actual = testSubject.toStructureTree()
        actual.bitSize shouldBe 24
        actual.toString() shouldBe """
            [u8; 3]: 24 bits (3 bytes)
            └── u8: 8 bits (1 bytes)
        """.trimIndent()
    }
}
