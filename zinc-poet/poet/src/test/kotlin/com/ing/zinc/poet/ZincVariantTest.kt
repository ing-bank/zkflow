package com.ing.zinc.poet

import com.ing.zinc.poet.ZincVariant.Companion.zincVariant
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincVariantTest {
    @Test
    fun `Builder should return ZincVariant successfully`() {
        val actual = zincVariant {
            name = "variant"
            ordinal = 1
        }
        actual.getName() shouldBe "variant"
        actual.getOrdinal() shouldBe 1
    }

    @Test
    fun `Builder should throw IllegalArgumentException when name not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincVariant {
                ordinal = 1
            }
        }
        exception.localizedMessage shouldBe "Required value `name` is null."
    }

    @Test
    fun `Builder should throw IllegalArgumentException when ordinal not set`() {
        val exception = shouldThrow<IllegalArgumentException> {
            zincVariant {
                name = "variant"
            }
        }
        exception.localizedMessage shouldBe "Required value `ordinal` is null."
    }
}
