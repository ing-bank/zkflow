package com.ing.zinc.poet

import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincComment.Companion.zincComment
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincFileTest {
    @Test
    fun `ZincFile generate should generate all parts`() {
        actual.generate() shouldBe """
            const TEST_CONSTANT = 0 as u8;
            fn test_function() -> bool {
                true
            }
            mod test_module;
            use test_module::test_use;
            struct TestStruct {
                test_field: bool,
            }
            impl TestStruct {
                fn test_method(
                    self,
                ) -> bool {
                    true
                }
            }
            enum TestEnum {
                TEST_VARIANT = 0,
            }
            type TestTypeDef = [bool; 2];

            // Test comment.
            // Another test comment.
            // A test comment from a list.
            // Another test comment from a list.
        """.trimIndent()
    }

    companion object {
        val actual = zincFile {
            constant {
                name = "TEST_CONSTANT"
                initialization = "0 as u8"
            }
            function {
                name = "test_function"
                returnType = ZincPrimitive.Bool
                body = "true"
            }
            mod {
                module = "test_module"
            }
            use {
                path = "test_module::test_use"
            }
            struct {
                name = "TestStruct"
                field {
                    name = "test_field"
                    type = ZincPrimitive.Bool
                }
            }
            impl {
                name = "TestStruct"
                method {
                    name = "test_method"
                    returnType = ZincPrimitive.Bool
                    body = "true"
                }
            }
            enum {
                name = "TestEnum"
                variant {
                    name = "TEST_VARIANT"
                    ordinal = 0
                }
            }
            type {
                name = "TestTypeDef"
                type = zincArray {
                    size = "2"
                    elementType = ZincPrimitive.Bool
                }
            }
            newLine()
            comment("Test comment.")
            add(zincComment("Another test comment."))
            addAll(
                listOf(
                    zincComment("A test comment from a list."),
                    zincComment("Another test comment from a list.")
                )
            )
        }
    }
}
