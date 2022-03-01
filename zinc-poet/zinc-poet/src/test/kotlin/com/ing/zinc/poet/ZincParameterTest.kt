package com.ing.zinc.poet

import com.ing.zinc.poet.ZincParameter.Companion.zincParameter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincParameterTest {
    @Test
    fun `Builder should create a ZincParameter successfully`() {
        val actual = zincParameter {
            name = "param1"
            type = ZincPrimitive.U8
            mutable = true
        }
        actual.getName() shouldBe "param1"
        actual.getType() shouldBe ZincPrimitive.U8
        actual.isMutable() shouldBe true
    }

    @Test
    fun `Builder should throw IllegalArgumentException when name not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincParameter {
                type = ZincPrimitive.U8
            }
        }
        exception.localizedMessage shouldBe "Required value `name` is null."
    }

    @Test
    fun `Builder should throw IllegalArgumentException when type not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincParameter {
                name = "param1"
            }
        }
        exception.localizedMessage shouldBe "Required value `type` is null."
    }
}
