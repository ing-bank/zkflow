package com.ing.zinc.poet

import com.ing.zinc.poet.ZincField.Companion.zincField
import com.ing.zinc.poet.ZincStruct.Companion.zincStruct
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincStructTest {
    private fun genField(index: Int) = zincField {
        name = "field$index"
        type = ZincPrimitive.U8
    }

    @Test
    fun `Builder should build a ZincStruct successfully`() {
        val actual = zincStruct {
            name = "SomeStruct"
            addField(genField(1))
            addFields(listOf(genField(2)))
            field {
                name = "field3"
                type = ZincPrimitive.U8
            }
        }
        actual.getName() shouldBe "SomeStruct"
        actual.getFields() shouldBe (1..3).map { genField(it) }
    }

    @Test
    fun `Builder should throw IllegalArgumentException when name not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincStruct { }
        }
        exception.localizedMessage shouldBe "Required value `name` is null."
    }

    @Test
    fun `generate should correctly generate a struct`() {
        val actual = zincStruct {
            name = "SomeStruct"
            addField(genField(1))
            addFields(listOf(genField(2)))
            field {
                name = "field3"
                type = ZincPrimitive.U8
            }
        }
        actual.generate() shouldBe """
            struct SomeStruct {
                field1: u8,
                field2: u8,
                field3: u8,
            }
        """.trimIndent()
    }
}
