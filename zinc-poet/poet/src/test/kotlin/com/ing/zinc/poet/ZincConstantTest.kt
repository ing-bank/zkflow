package com.ing.zinc.poet

import com.ing.zinc.poet.ZincConstant.Companion.zincConstant
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincConstantTest {
    @Test
    fun `Builder should create a ZincConstant successfully`() {
        val actual = zincConstant {
            name = "CONSTANT"
            initialization = "0"
        }
        actual.getName() shouldBe "CONSTANT"
        actual.getType() shouldBe null
        actual.getInitialization() shouldBe "0"
    }

    @Test
    fun `Builder should create a ZincConstant with type successfully`() {
        val actual = zincConstant {
            name = "CONSTANT"
            type = ZincPrimitive.U8
            initialization = "0"
        }
        actual.getName() shouldBe "CONSTANT"
        actual.getType() shouldBe ZincPrimitive.U8
        actual.getInitialization() shouldBe "0"
    }

    @Test
    fun `Builder should throw IllegalArgumentException when name not set`() {
        val exceptionn = shouldThrow<IllegalArgumentException> {
            zincConstant {
                initialization = "0"
            }
        }
        exceptionn.localizedMessage shouldBe "Required value `name` is null."
    }

    @Test
    fun `Builder should throw IllegalArgumentException when initialization not set`() {
        val exceptionn = shouldThrow<IllegalArgumentException> {
            zincConstant {
                name = "CONSTANT"
            }
        }
        exceptionn.localizedMessage shouldBe "Required value `initialization` is null."
    }

    @Test
    fun `generate should generate a correct constant`() {
        val actual = zincConstant {
            name = "CONSTANT"
            type = ZincPrimitive.U8
            initialization = "0"
        }
        actual.generate() shouldBe "const CONSTANT: u8 = 0;"
    }

    @Test
    fun `generate should generate a correct constant without type`() {
        val actual = zincConstant {
            name = "CONSTANT"
            initialization = "0"
        }
        actual.generate() shouldBe "const CONSTANT = 0;"
    }

    @Test
    fun `generate should generate a correct constant with multi-line init`() {
        val actual = zincConstant {
            name = "CONSTANT"
            type = ZincPrimitive.U8
            initialization = """
                offset
                + length
            """.trimIndent()
            comment = "Just some constant"
        }
        actual.generate() shouldBe """
            // Just some constant
            const CONSTANT: u8
                = offset
                + length;
        """.trimIndent()
    }
}
