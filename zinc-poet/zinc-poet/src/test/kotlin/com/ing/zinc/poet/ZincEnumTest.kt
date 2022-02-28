package com.ing.zinc.poet

import com.ing.zinc.poet.ZincEnum.Companion.zincEnum
import com.ing.zinc.poet.ZincVariant.Companion.zincVariant
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincEnumTest {
    @Test
    fun `Builder should correctly build an Enum`() {
        val actual = zincEnum {
            name = "SomeEnum"
            addVariant(zincVariant { name = "SOMETHING"; ordinal = 1 })
            addVariants(listOf(zincVariant { name = "ANYTHING"; ordinal = 2 }))
            variant { name = "EVERYTHING"; ordinal = 3 }
        }
        actual.getId() shouldBe "SomeEnum"
        actual.getName() shouldBe "SomeEnum"
        actual.getVariants() shouldBe listOf(
            zincVariant { name = "SOMETHING"; ordinal = 1 },
            zincVariant { name = "ANYTHING"; ordinal = 2 },
            zincVariant { name = "EVERYTHING"; ordinal = 3 },
        )
    }

    @Test
    fun `Builder should throw IllegalArgumentException when name not set`() {
        val exceptionn = shouldThrow<IllegalArgumentException> {
            zincEnum {}
        }
        exceptionn.localizedMessage shouldBe "Required value `name` is null."
    }

    @Test
    fun `generate should correctly generate en enum`() {
        val actual = zincEnum {
            name = "SomeEnum"
            addVariant(zincVariant { name = "SOMETHING"; ordinal = 1 })
            addVariants(listOf(zincVariant { name = "ANYTHING"; ordinal = 2 }))
            variant { name = "EVERYTHING"; ordinal = 3 }
        }
        actual.generate() shouldBe """
            enum SomeEnum {
                SOMETHING = 1,
                ANYTHING = 2,
                EVERYTHING = 3,
            }
        """.trimIndent()
    }
}
