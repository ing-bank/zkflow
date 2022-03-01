package com.ing.zinc.poet

import com.ing.zinc.poet.ZincUse.Companion.zincUse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincUseTest {
    @Test
    fun `Builder should build a ZincUse successfully`() {
        val actual = zincUse {
            path = "some_path"
        }
        actual.getPath() shouldBe "some_path"
    }

    @Test
    fun `Builder should throw IllegalArgumentException when path not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincUse { }
        }
        exception.localizedMessage shouldBe "Required value `path` is null."
    }

    @Test
    fun `generate should correctly generate a use statement`() {
        val actual = zincUse {
            path = "some_path"
        }
        actual.generate() shouldBe "use some_path;"
    }
}
