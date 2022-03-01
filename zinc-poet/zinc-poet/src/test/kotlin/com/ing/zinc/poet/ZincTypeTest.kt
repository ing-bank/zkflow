package com.ing.zinc.poet

import com.ing.zinc.poet.ZincType.Companion.id
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincTypeTest {
    @Test
    fun `id should construct a TypeIdentifier`() {
        val actual = id("Hello")

        actual.getId() shouldBe "Hello"
    }

    @Test
    fun `Self should construct the Self TypeIdentifier`() {
        val actual = Self

        actual.getId() shouldBe "Self"
    }
}
