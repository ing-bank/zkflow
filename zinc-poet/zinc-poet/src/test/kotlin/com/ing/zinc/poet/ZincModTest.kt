package com.ing.zinc.poet

import com.ing.zinc.poet.ZincMod.Companion.zincMod
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincModTest {
    @Test
    fun `Builder should build a ZincMod successfully`() {
        val actual = zincMod {
            module = "some_module"
        }
        actual.getModule() shouldBe "some_module"
    }

    @Test
    fun `Builder should throw IllegalArgumentException when module not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincMod { }
        }
        exception.localizedMessage shouldBe "Required value `module` is null."
    }

    @Test
    fun `generate should correctly generat a mod statement`() {
        val actual = zincMod {
            module = "some_module"
        }
        actual.generate() shouldBe "mod some_module;"
    }
}
