package com.ing.zinc.poet

import com.ing.zinc.poet.ZincComment.Companion.zincComment
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincCommentTest {
    @Test
    fun `Builder should build a Comment successfully`() {
        val actual = zincComment("only line")
        actual.getComment() shouldBe "only line"
    }

    @Test
    fun `generate should generate a correct multi-line comment`() {
        val actual = zincComment(
            """
                first line
                second line
            """.trimIndent()
        )
        actual.generate() shouldBe """
            // first line
            // second line
        """.trimIndent()
    }
}
