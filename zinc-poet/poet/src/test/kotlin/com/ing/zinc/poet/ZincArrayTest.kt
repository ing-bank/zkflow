package com.ing.zinc.poet

import com.ing.zinc.poet.ZincArray.Companion.zincArray
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException

internal class ZincArrayTest {
    @Test
    fun `Builder should correctly build an array`() {
        val actual = zincArray {
            elementType = ZincPrimitive.I32
            size = "8"
        }
        actual.getId() shouldBe "[i32; 8]"
        actual.getElementType() shouldBe ZincPrimitive.I32
        actual.getSize() shouldBe "8"
    }

    @Test
    fun `Builder should throw IllegalArgumentException when size not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincArray {
                elementType = ZincPrimitive.I32
            }
        }
        exception.message shouldBe "Required value `size` is null."
    }

    @Test
    fun `Builder should throw IllegalArgumentException when elementType not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincArray {
                size = "8"
            }
        }
        exception.message shouldBe "Required value `elementType` is null."
    }
}
