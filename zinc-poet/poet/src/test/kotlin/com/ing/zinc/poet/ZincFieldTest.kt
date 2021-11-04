package com.ing.zinc.poet

import com.ing.zinc.poet.ZincField.Companion.zincField
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincFieldTest {
    @Test
    fun `Builder should return ZincField successfully`() {
        val actual = zincField {
            name = "field"
            type = ZincPrimitive.U8
        }
        actual.getName() shouldBe "field"
        actual.getType() shouldBe ZincPrimitive.U8
    }

    @Test
    fun `Builder should throw IllegalArgumentException when name not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincField {
                type = ZincPrimitive.U8
            }
        }
        exception.localizedMessage shouldBe "Required value `name` is null."
    }

    @Test
    fun `Builder should throw IllegalArgumentException when type not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincField {
                name = "field"
            }
        }
        exception.localizedMessage shouldBe "Required value `type` is null."
    }
}
