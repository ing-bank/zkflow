package com.ing.zinc.poet

import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincImpl.Companion.zincImpl
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincImplTest {
    private fun genFunction(index: Int) = zincFunction {
        name = "function$index"
        returnType = ZincPrimitive.Bool
        body = "true"
    }

    private fun genMethod(index: Int) = zincMethod {
        name = "method$index"
        returnType = ZincPrimitive.Bool
        body = "true"
    }

    @Test
    fun `Builder should generate a ZincImpl successfully`() {
        val actual = zincImpl {
            name = "SomeClass"
            addFunction(genFunction(1))
            addFunctions(listOf(genFunction(2)))
            function {
                name = "function3"
                returnType = ZincPrimitive.Bool
                body = "true"
            }
            addMethod(genMethod(1))
            addMethods(listOf(genMethod(2)))
            method {
                name = "method3"
                returnType = ZincPrimitive.Bool
                body = "true"
            }
        }
        actual.getName() shouldBe "SomeClass"
        actual.getMethods() shouldBe (1..3).map { genFunction(it) } + (1..3).map { genMethod(it) }
    }

    @Test
    fun `Builder should throw IllegalArgumentException when name not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincImpl { }
        }
        exception.localizedMessage shouldBe "Required value `name` is null."
    }

    @Test
    fun `generate should correctly generate an impl block`() {
        val actual = zincImpl {
            name = "SomeClass"
            addFunction(genFunction(1))
            addFunctions(listOf(genFunction(2)))
            function {
                name = "function3"
                returnType = ZincPrimitive.Bool
                body = "true"
            }
            addMethod(genMethod(1))
            addMethods(listOf(genMethod(2)))
            method {
                name = "method3"
                returnType = ZincPrimitive.Bool
                body = "true"
            }
        }
        actual.generate() shouldBe """
            impl SomeClass {
                fn function1() -> bool {
                    true
                }
                
                fn function2() -> bool {
                    true
                }
                
                fn function3() -> bool {
                    true
                }
                
                fn method1(self) -> bool {
                    true
                }
                
                fn method2(self) -> bool {
                    true
                }
                
                fn method3(self) -> bool {
                    true
                }
            }
        """.trimIndent()
    }
}
