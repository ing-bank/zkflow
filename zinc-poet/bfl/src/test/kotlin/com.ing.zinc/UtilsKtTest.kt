package com.ing.zinc

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class UtilsKtTest {
    @Test
    fun `requireNotEmpty should return input when not empty`() {
        val actual = requireNotEmpty(listOf(1, 2, 3)) {
            "Bummer"
        }

        actual shouldContainInOrder listOf(1, 2, 3)
    }

    @Test
    fun `requireNotEmpty should throw exception when empty`() {
        val ex = shouldThrow<IllegalArgumentException> {
            requireNotEmpty(emptyList<Unit>()) {
                "Bummer"
            }
        }
        ex.message shouldBe "Bummer"
    }
}
