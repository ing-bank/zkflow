package com.ing.zinc.poet

import com.ing.zinc.poet.ZincNewline.Companion.ImmutableZincNewline
import com.ing.zinc.poet.ZincNewline.Companion.zincNewline
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincNewlineTest {
    @Test
    fun `ZincNewline can be obtained`() {
        val actual = zincNewline()
        actual.javaClass shouldBe ImmutableZincNewline.javaClass
    }

    @Test
    fun `generate should correctly generate a newline`() {
        zincNewline().generate() shouldBe ""
    }
}
