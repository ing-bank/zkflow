package com.ing.zinc.poet

import com.ing.zinc.poet.ZincComment.Companion.zincComment
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincParameter.Companion.zincParameter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincFunctionTest {
    @Test
    fun `Builder should generate a ZincFunction successfully`() {
        val actual = zincFunction {
            name = "some_function"
            returnType = ZincPrimitive.Bool
            body = "true"
            comment = """
                Just some test function.
                For fun.
            """.trimIndent()
            addParameter(genParam(1))
            addParameters(listOf(genParam(2)))
            parameter { name = "param3"; type = ZincPrimitive.U8 }
        }
        actual.getName() shouldBe "some_function"
        actual.getReturnType() shouldBe ZincPrimitive.Bool
        actual.getBody() shouldBe "true"
        actual.getComment() shouldBe zincComment("Just some test function.\nFor fun.")
        actual.getParameters() shouldBe (1..3).map { genParam(it) }
    }

    private fun genParam(index: Int) = zincParameter {
        name = "param$index"
        type = ZincPrimitive.U8
    }

    @Test
    fun `Builder should throw IllegalArgumentException when name not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincFunction {
                returnType = ZincPrimitive.Bool
                body = "true"
            }
        }
        exception.localizedMessage shouldBe "Required value `name` is null."
    }

    @Test
    fun `Builder should throw IllegalArgumentException when returnType not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincFunction {
                name = "some_function"
                body = "true"
            }
        }
        exception.localizedMessage shouldBe "Required value `returnType` is null."
    }

    @Test
    fun `Builder should throw IllegalArgumentException when body not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincFunction {
                name = "some_function"
                returnType = ZincPrimitive.Bool
            }
        }
        exception.localizedMessage shouldBe "Required value `body` is null."
    }

    @Test
    fun `generate should correctly generate a function`() {
        val actual = zincFunction {
            name = "some_function"
            returnType = ZincPrimitive.Bool
            body = "true"
            comment = """
                Just some test function.
                For fun.
            """.trimIndent()
            addParameter(genParam(1))
            addParameters(listOf(genParam(2)))
            parameter { name = "param3"; type = ZincPrimitive.U8 }
        }
        actual.generate() shouldBe """
            // Just some test function.
            // For fun.
            fn some_function(param1: u8, param2: u8, param3: u8) -> bool {
                true
            }
        """.trimIndent()
    }
}
