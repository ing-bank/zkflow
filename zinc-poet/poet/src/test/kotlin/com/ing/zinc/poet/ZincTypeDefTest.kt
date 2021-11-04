package com.ing.zinc.poet

import com.ing.zinc.poet.ZincTypeDef.Companion.zincTypeDef
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincTypeDefTest {
    @Test
    fun `Builder should build a ZincTypeDef successfully`() {
        val actual = zincTypeDef {
            name = "typeDef"
            type = ZincPrimitive.U8
        }
        actual.getName() shouldBe "typeDef"
        actual.getType() shouldBe ZincPrimitive.U8
    }

    @Test
    fun `Builder should throw IllegalArgumentException when name not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincTypeDef {
                type = ZincPrimitive.U8
            }
        }
        exception.localizedMessage shouldBe "Required value `name` is null."
    }

    @Test
    fun `Builder should throw IllegalArgumentException when type not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincTypeDef {
                name = "typeDef"
            }
        }
        exception.localizedMessage shouldBe "Required value `type` is null."
    }

    @Test
    fun `generate should correctly generate a type definition`() {
        val actual = zincTypeDef {
            name = "typeDef"
            type = ZincPrimitive.U8
        }
        actual.generate() shouldBe "type typeDef = u8;"
    }
}
