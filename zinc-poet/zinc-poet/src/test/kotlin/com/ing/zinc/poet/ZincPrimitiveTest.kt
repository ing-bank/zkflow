package com.ing.zinc.poet

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZincPrimitiveTest {
    @Test
    fun `getId should return actual zinc type identifier`() {
        for (value in ZincPrimitive.values()) {
            testZincPrimitiveId(value)
        }
    }

    private fun testZincPrimitiveId(value: ZincPrimitive) {
        when (value) {
            ZincPrimitive.U8 -> value.getId() shouldBe "u8"
            ZincPrimitive.U16 -> value.getId() shouldBe "u16"
            ZincPrimitive.U24 -> value.getId() shouldBe "u24"
            ZincPrimitive.U32 -> value.getId() shouldBe "u32"
            ZincPrimitive.U64 -> value.getId() shouldBe "u64"
            ZincPrimitive.U128 -> value.getId() shouldBe "u128"
            ZincPrimitive.I8 -> value.getId() shouldBe "i8"
            ZincPrimitive.I16 -> value.getId() shouldBe "i16"
            ZincPrimitive.I32 -> value.getId() shouldBe "i32"
            ZincPrimitive.I64 -> value.getId() shouldBe "i64"
            ZincPrimitive.I128 -> value.getId() shouldBe "i128"
            ZincPrimitive.Bool -> value.getId() shouldBe "bool"
            ZincPrimitive.Unit -> value.getId() shouldBe "()"
        }
    }
}
