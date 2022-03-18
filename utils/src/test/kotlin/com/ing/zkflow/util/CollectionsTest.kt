package com.ing.zkflow.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CollectionsTest {
    @Test
    fun `extendTo should extend a list with default values`() {
        listOf(1, 2, 3).extendTo(5, 5) shouldBe listOf(1, 2, 3, 5, 5)
    }

    @Test
    fun `extendTo actual size should return equal list`() {
        listOf(1, 2, 3).extendTo(3, 5) shouldBe listOf(1, 2, 3)
    }

    @Test
    fun `extendTo smaller size should throw an exception`() {
        val ex = shouldThrow<IllegalArgumentException> {
            listOf(1, 2, 3).extendTo(2, 5)
        }
        ex.message shouldBe "List size (3) is larger than requested size (2)."
    }

    @Test
    fun `shrinkTo smaller size should return a smaller list`() {
        listOf(1, 2, 3, 4, 5).shrinkTo(3) shouldBe listOf(1, 2, 3)
    }

    @Test
    fun `shrinkTo actual size should return equal list`() {
        listOf(1, 2, 3).shrinkTo(3) shouldBe listOf(1, 2, 3)
    }

    @Test
    fun `shrinkTo larger list should throw an exception`() {
        val ex = shouldThrow<IllegalArgumentException> {
            listOf(1, 2, 3).shrinkTo(5)
        }
        ex.message shouldBe "Requested size (5) is larger than actual size (3)."
    }
}
